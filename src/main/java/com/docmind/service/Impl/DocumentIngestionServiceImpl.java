package com.docmind.service.Impl;

import com.docmind.config.AppProperties;
import com.docmind.exception.DocumentProcessingException;
import com.docmind.model.DocumentMetadata;
import com.docmind.repository.DocumentMetadataRepository;
import com.docmind.service.DocumentIngestionService;
import com.docmind.utils.DocumentStatus;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;


import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestionServiceImpl implements DocumentIngestionService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentIngestionServiceImpl.class);

    private final VectorStore vectorStore;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final AppProperties appProperties;

    @Override
    public int ingest(DocumentMetadata metadata, List<Document> parsedDocs) {
        logger.info("Ingesting document [id={}, name={}, pages={}]", metadata.getId(),metadata.getFilename(), metadata.getFileSize());

        try{
            metadata.setStatus(DocumentStatus.PROCESSING);
            metadata.setTotalPages(parsedDocs.size());

            // 1. Text chunking using TokenTextSplitter
            TokenTextSplitter tokenTextSplitter = TokenTextSplitter.builder()
                    .withChunkSize(appProperties.getRag().getChunkSize())
                    .withMinChunkSizeChars(appProperties.getRag().getMinChunkSizeChars())
                    .withMinChunkLengthToEmbed(appProperties.getRag().getMinChunkLengthToEmbed())
                    .withMaxNumChunks(appProperties.getRag().getMaxNumChunks())
                    .withKeepSeparator(true)
                    .build();

            List<Document> chunks = tokenTextSplitter.apply(parsedDocs);
            if(chunks.isEmpty()){
                metadata.setStatus(DocumentStatus.FAILED);
                metadata.setErrorMessage("Document appears to be empty or unsaleable");
                documentMetadataRepository.save(metadata);
                return 0;
            }

            // 2. Meta data enrichment on each chunk
            List<Document> enrichedChunks = new ArrayList<>();
            for(int i=0; i<chunks.size(); i++){
                Document chunk = chunks.get(i);

                Map<String, Object> enrichedMetadata = new HashMap<>(chunk.getMetadata());
                enrichedMetadata.put("documentId", metadata.getId().toString());
                enrichedMetadata.put("fileName", metadata.getFilename());
                enrichedMetadata.put("contentType", metadata.getContentType());
                enrichedMetadata.put("chunkIndex", i);

                Object pageNumber = chunk.getMetadata().get("page_number");
                if(pageNumber == null){
                    pageNumber = chunk.getMetadata().get("pageNumber");
                }

                if(pageNumber != null){
                    enrichedMetadata.put("pageNumber", pageNumber);
                }

                Document enrichedDoc = new Document(Objects.requireNonNull(chunk.getText()), enrichedMetadata);
                enrichedChunks.add(enrichedDoc);
            }

            // 3. Write chunks and embedding to pg vector
            logger.info("Writing {} vector chunks to PgVectorStore for document: {}", enrichedChunks.size(), metadata.getFilename());
            vectorStore.add(enrichedChunks);

            // 4. Update document status to index
            metadata.setStatus(DocumentStatus.INDEXED);
            metadata.setTotalChunks(enrichedChunks.size());
            metadata.setErrorMessage(null);

            documentMetadataRepository.save(metadata);
            logger.info("Successfully indexed document [id={}, name={}, chunks={}]", metadata.getId(), metadata.getFilename(), enrichedChunks.size());

            return enrichedChunks.size();
        }
        catch (Exception ex){
            log.error("Failed to ingest document into vector store: {}", metadata.getFilename(), ex);
            metadata.setStatus(DocumentStatus.FAILED);
            metadata.setErrorMessage(ex.getMessage());

            documentMetadataRepository.save(metadata);
            throw new DocumentProcessingException("Failed to index document: " + ex.getMessage(), ex);
        }


    }
}
