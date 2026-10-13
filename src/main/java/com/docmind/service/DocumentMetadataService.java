package com.docmind.service;

import com.docmind.model.DocumentMetadata;
import com.docmind.model.User;
import com.docmind.response.DocumentMetadataResponse;
import com.docmind.response.DocumentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;


public interface DocumentMetadataService {

    DocumentResponse uploadAndProcess(MultipartFile file, User user);

    List<DocumentResponse> uploadMultipleDocuments(List<MultipartFile> files, User user);

    List<DocumentMetadataResponse> getAllDocuments();
    DocumentMetadataResponse getDocumentById(UUID id);
    List<DocumentMetadataResponse> getAllDocumentsByUser(User user);

    void deleteDocument(UUID id);
}
