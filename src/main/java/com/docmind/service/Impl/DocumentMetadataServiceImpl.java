package com.docmind.service.Impl;

import com.docmind.exception.DocumentProcessingException;
import com.docmind.exception.ResourceNotFoundException;
import com.docmind.model.DocumentMetadata;
import com.docmind.model.User;
import com.docmind.repository.DocumentMetadataRepository;
import com.docmind.response.DocumentMetadataResponse;
import com.docmind.response.DocumentResponse;
import com.docmind.service.DocumentIngestionService;
import com.docmind.service.DocumentMetadataService;
import com.docmind.service.DocumentParserService;
import com.docmind.utils.DocumentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.modelmapper.ModelMapper;


import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentMetadataServiceImpl implements DocumentMetadataService {
    private static final Logger logger = LoggerFactory.getLogger(DocumentMetadataServiceImpl.class);
    private final DocumentMetadataRepository documentMetadataRepository;

    private final DocumentParserService documentParserService;
    private final DocumentIngestionService documentIngestionService;
    private final ModelMapper modelMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public DocumentResponse uploadAndProcess(MultipartFile file, User user) {
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octat-stream";

        // document meta data create
        DocumentMetadata documentMetadata = DocumentMetadata.builder()
                .filename(fileName)
                .contentType(contentType)
                .status(DocumentStatus.UPLOADING)
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .user(user)
                .build();

        // save the document meta data
        documentMetadata = documentMetadataRepository.save(documentMetadata);

        List<Document> parsedDocs = null;
        int chunksCreated = 0;

        try {
            // parse the file
            parsedDocs = documentParserService.parse(file);    // Document Loader

            // ingest service
            chunksCreated = documentIngestionService.ingest(documentMetadata, parsedDocs);    // Text Splitter
        }
        catch (DocumentProcessingException e){
            logger.info("Document metadata deleting due to fail processing");
            documentMetadataRepository.delete(documentMetadata);
            throw e;
        }

        return DocumentResponse.builder()
                .id(documentMetadata.getId())
                .fileName(documentMetadata.getFilename())
                .fileSize(documentMetadata.getFileSize())
                .chunksCreated(chunksCreated)
                .status(documentMetadata.getStatus())
                .message("Document successfully processed and indexed.")
                .build();
    }

    @Override
    public List<DocumentResponse> uploadMultipleDocuments(List<MultipartFile> files, User user) {
        List<DocumentResponse> responseDtos = new ArrayList<>();

        for (MultipartFile file : files) {
            DocumentResponse result = this.uploadAndProcess(file, user);
            responseDtos.add(result);
        }

        return responseDtos;
    }

    @Override
    public List<DocumentMetadataResponse> getAllDocuments() {
        List<DocumentMetadata> documentMetadata = documentMetadataRepository.findAllByOrderByCreatedAtDesc();
        return documentMetadata.stream()
                .map(documentMetadata1 -> modelMapper.map(documentMetadata1, DocumentMetadataResponse.class))
                .toList();
    }

    public List<DocumentMetadataResponse> getAllDocumentsByUser(User user) {
        List<DocumentMetadata> documentMetadata = documentMetadataRepository.findByUserOrderByCreatedAtDesc(user);
        return documentMetadata.stream()
                .map(documentMetadata1 -> modelMapper.map(documentMetadata1, DocumentMetadataResponse.class))
                .toList();
    }


    @Override
    public DocumentMetadataResponse getDocumentById(UUID id) {
        DocumentMetadata documentMetadata = documentMetadataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));

        return modelMapper.map(documentMetadata, DocumentMetadataResponse.class);
    }

    @Override
    @Transactional
    public void deleteDocument(UUID id) {
        DocumentMetadata documentMetadata = documentMetadataRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document with given id not found !!"));

        documentMetadataRepository.delete(documentMetadata);

        // Delete the vector entries
        try{
            String deleteVectorsSql = "DELETE FROM vector_store WHERE metadata->>'documentId' = ?";
            int deletedCount = jdbcTemplate.update(deleteVectorsSql, id.toString());

            log.info("Deleted {} vector chunks for document id {} ", deletedCount, id);
        }
        catch (Exception e) {
            log.warn("cloud not delete vectors from vector store directly: {}", e.getMessage());
        }

    }

}
