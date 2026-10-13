package com.docmind.config;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.join.ConcatenationDocumentJoiner;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RagConfig {
    private final VectorStore vectorStore;
    //    private final OllamaChatModel ollamaChatModel;
    private final OpenAiChatModel ollamaChatModel;

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(){
        return RetrievalAugmentationAdvisor.builder()
                // PRE RETRIEVAL
                .queryTransformers(
                        RewriteQueryTransformer.builder()
                                .chatClientBuilder(ChatClient.builder(ollamaChatModel))
                                .build(),

                        TranslationQueryTransformer.builder()
                                .chatClientBuilder(ChatClient.builder(ollamaChatModel))
                                .targetLanguage("english")
                                .build()
                )
                .queryExpander(
                        MultiQueryExpander.builder()
                                .chatClientBuilder(ChatClient.builder(ollamaChatModel))
                                .build()
                )
                // RETRIEVAL
                .documentRetriever(
                        VectorStoreDocumentRetriever.builder()
                                .vectorStore(vectorStore)
                                .topK(5)
                                .similarityThreshold(0.3)
                                .build()
                )
                // POST RETRIEVAL
                .documentJoiner(new ConcatenationDocumentJoiner())
                .queryAugmenter(
                        ContextualQueryAugmenter.builder()
                                .allowEmptyContext(true)
                                .build()
                )

                .build();
    }

}
