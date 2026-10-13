package com.docmind.service.Impl;

import com.docmind.config.AppProperties;
import com.docmind.dto.CitationDto;
import com.docmind.dto.SearchResultDto;
import com.docmind.model.Conversation;
import com.docmind.model.User;
import com.docmind.repository.ConversationRepository;
import com.docmind.request.ChatRequest;
import com.docmind.request.SearchRequests;
import com.docmind.response.ChatResponse;
import com.docmind.service.RagService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.ai.vectorstore.SearchRequest;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.*;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagServiceImpl implements RagService {
    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);

    private final VectorStore vectorStore;
    private final AppProperties appProperties;
    private final ChatClient chatClient;

    private final ConversationRepository conversationRepository;

    // To ask anything related to document
    public ChatResponse askQuestion(ChatRequest request, User user){
        long startTime = System.currentTimeMillis();

        String conversationId = request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString();
        ensureConversationExists(conversationId, user, request.getQuestion());

        log.info("Processing query: '{}', scoped documentId: {}", request.getQuestion(), request.getDocumentId());

        List<Document> similarDocuments = this.retrieveRelevantDocuments(request.getQuestion(), request.getDocumentId(), request.getTopK(), request.getMinSimilarity(), user);
        List<CitationDto> citations = similarDocuments.stream()
                .map(this::mapToCitation)
                .toList();

        String contextText = buildContextString(similarDocuments);

        // you have to use conversationId to remember the conversation
        // ChatMemory
        // ChatMemoryRepository
        String answer = this.chatClient
                .prompt()
                .system(s -> s.param("doc_context", contextText))
                .user(request.getQuestion())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getConversationId()))
                .call()
                .content();

        long responseTime = System.currentTimeMillis() - startTime;
        log.info("Completed Q&A in {} ms with {} citations", responseTime, citations.size());

        return ChatResponse.builder()
                .answer(answer)
                .conversationId(request.getConversationId())
                .citations(citations)
                .responseTimeMs(responseTime)
                .build();
    }

    // this streams
    public Flux<String> streamQuestionAnswer(ChatRequest request, User user) {
        log.info("Streaming query: '{}'", request.getQuestion());

        String conversationId = request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString();
        request.setConversationId(conversationId);
        ensureConversationExists(conversationId, user, request.getQuestion());

        List<Document> relevantDocuments = this.retrieveRelevantDocuments(request.getQuestion(), request.getDocumentId(), request.getTopK(), request.getMinSimilarity(), user);
        String contextText = buildContextString(relevantDocuments);

        return chatClient.prompt()
                .system(s-> s.param("doc_context",contextText))
                .user(request.getQuestion())
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.getConversationId()))
                .stream()
                .content()
                .concatWith(Flux.just("[DONE]"));

    }


    private String buildPrompt(@NotBlank(message = "Question cannot be empty") String question, String contextText) {
        if (contextText != null && !contextText.isBlank()) {
            return String.format("""
                           Document Context:
                           ---------------------
                           %s
                           ---------------------                                        
                           User Message / Question: %s             
                           Instructions:
                           - If the user's question relates to the document context above, prioritize answering using that context and reference key sections.
                           - If the user is asking a general question, greeting, or discussing topics beyond the document context, respond helpfully and conversationally using your general knowledge while weaving in relevant document context if applicable
                    
                    """, contextText, question);
        }
        else {
            return String.format("""
                       User Message / Question:
                       %s                         
                       Instructions:
                          - Respond helpfully, accurately, and conversationally to the user's message using your broad knowledge base.
                    
                    """, question);
        }


    }





    private String buildContextString(List<Document> similarDocuments) {
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "";
        }

        return similarDocuments.stream().map(doc -> {
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
            Object page = doc.getMetadata().getOrDefault("pageNumber", "N/A");

            return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getText());
        }).collect(Collectors.joining("\n\n---\n\n"));

    }



    private void ensureConversationExists(String conversationId, User user, @NotBlank(message = "Question cannot be empty") String question){

        conversationRepository.findByIdAndUser(conversationId, user)
                .orElseGet(() -> {
                    String title = question.length() > 50 ? question.substring(0, 47) + "....." : question;
                    return conversationRepository.save(
                            Conversation.builder().id(conversationId)
                                    .title(title)
                                    .user(user)
                                    .createdAt(LocalDateTime.now())
                                    .updatedAt(LocalDateTime.now())
                                    .build()
                    );
                });
    }


    public SearchResultDto searchSimilarChunks(SearchRequests request, User user){
        List<Document> matchedDocs = retrieveRelevantDocuments(request.getQuery(), request.getDocumentId(), request.getTopK(), request.getSimilaritySearch(), user);

        List<CitationDto> citations = matchedDocs.stream()
                .map(this::mapToCitation)
                .toList();

        return SearchResultDto.builder()
                .query(request.getQuery())
                .totalMatches(citations.size())
                .matches(citations)
                .build();
    }



    private CitationDto mapToCitation(Document document){
        Map<String, Object> meta = document.getMetadata();
        UUID docId = null;

        if(meta.get("documentId") != null){
            try {
                docId = UUID.fromString(meta.get("documentId").toString());
            }
            catch (Exception ignore) {

            }
        }

        Integer chunkIndex = null;
        if(meta.get("chunkIndex") instanceof Number n){
            chunkIndex = n.intValue();
        }

        Integer pageNumber = null;
        if(meta.get("pageNumber") instanceof Number n){
            pageNumber = n.intValue();
        }
        else if(meta.get("page_number") instanceof Number n){
            pageNumber = n.intValue();
        }

        Double score = null;
        if (meta.get("distance") instanceof Number n) {
            score = 1.0 - n.doubleValue();
        }

        return CitationDto.builder()
                .documentId(docId)
                .fileName((String) meta.getOrDefault("fileName", "Unknown"))
                .chunkIndex(chunkIndex)
                .pageNumber(pageNumber)
                .snippet(document.getText())
                .similarityScore(score)
                .metadata(meta)
                .build();

    }

    // This is very important document that fetches the similar result from vector db
    private List<Document> retrieveRelevantDocuments(@NotBlank(message = "Query cannot be empty") String query, UUID documentId, Integer topK, Double similaritySearch, User user) {
        int effectiveTopK = (topK != null && topK > 0) ? topK : appProperties.getRag().getTopK();
        double effectiveSimilarity = (similaritySearch != null) ? similaritySearch : appProperties.getRag().getSimilarityThreshold();

        SearchRequest.Builder searchRequest = SearchRequest.builder()
                .query(query)
                .topK(effectiveTopK);

        if (effectiveSimilarity > 0.0) {
            searchRequest.similarityThreshold(effectiveSimilarity);
        }

        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        Filter.Expression filterExpression;
        if(documentId != null){
            log.info("Filtering from document: ");

            filterExpression = filterExpressionBuilder.and(
                    filterExpressionBuilder.eq("userId", user.getId().toString()),
                    filterExpressionBuilder.eq("documentId", documentId.toString())
            ).build();
        }
        else {
            filterExpression = filterExpressionBuilder.eq("userId", user.getId().toString()).build();
        }

        searchRequest.filterExpression(filterExpression);

        try {
            List<Document> documents = vectorStore.similaritySearch(searchRequest.build());
            assert documents != null;
            log.info("Retrieved {} chunks for query: '{}' (scoped docId: {})", documents.size(), query, documentId);
            return documents;
        }
        catch (Exception e) {
            log.error("Similarity search failed for query: '{}'", query, e);
            return Collections.emptyList();
        }

    }

}
