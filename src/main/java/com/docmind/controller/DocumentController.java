package com.docmind.controller;

import com.docmind.model.DocumentMetadata;
import com.docmind.model.User;
import com.docmind.response.ApiResponse;
import com.docmind.response.DocumentMetadataResponse;
import com.docmind.response.DocumentResponse;
import com.docmind.service.DocumentMetadataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;

import java.util.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/documents")
@Tag(
        name = "Document Management",
        description = "Endpoints for uploading, listing and managing documents and their vector embeddings"
)
public class DocumentController {
    private final DocumentMetadataService documentMetadataService;

//    @GetMapping
//    public String getDocuments() {
//        return "Documents API working";
//    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload and index a document(PDF, DOCX, TEXT, MD, CSV)",
            description = "This api is used to upload and index documents files."
    )
    public ApiResponse<DocumentResponse> uploadDocument(@RequestParam("file") MultipartFile file, Authentication authentication){
        log.info("document uploading started:");
        User user = (User) authentication.getPrincipal();

        // Process the files
        DocumentResponse documentResponse = this.documentMetadataService.uploadAndProcess(file, user);
        return ApiResponse.success(documentResponse);
    }


    // api to upload multiple documents
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload and index multiple documents simultaneously",
            description = "This api is used to upload and index multiple documents."
    )
    public ApiResponse<List<DocumentResponse>> uploadMultiple(@RequestParam("files") List<MultipartFile> files, Authentication authentication){
        User user = (User) authentication.getPrincipal();

        List<DocumentResponse> responses = this.documentMetadataService.uploadMultipleDocuments(files, user);
        return ApiResponse.success(responses);
    }

    // list all uploaded documents
    // Method for admin
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    @Operation(summary = "List all uploaded documents and their indexing status")
    public ApiResponse<List<DocumentMetadataResponse>> getAllDocuments(){
        List<DocumentMetadataResponse> documents = documentMetadataService.getAllDocuments();
        return ApiResponse.success(documents);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get metadata of a specific document by ID")
    public ApiResponse<DocumentMetadataResponse> getDocumentById(@PathVariable UUID id) {
        DocumentMetadataResponse documentMetadataResponse = documentMetadataService.getDocumentById(id);
        return ApiResponse.success(documentMetadataResponse);
    }

    // List all uploaded documents of logged-in user
    @GetMapping("/user")
    @Operation(summary = "List all uploaded documents and their indexing status of the logged user")
    public ApiResponse<List<DocumentMetadataResponse>> getAllDocumentOfLoggedInUser(Authentication authentication){
        User user = (User)authentication.getPrincipal();
        List<DocumentMetadataResponse> documentMetadataResponses = documentMetadataService.getAllDocumentsByUser(user);
        return ApiResponse.success(documentMetadataResponses);
    }


    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a document and purge its vector embeddings from vector store")
    public ApiResponse<Void> deleteDocument(@PathVariable UUID id) {
        documentMetadataService.deleteDocument(id);
        return ApiResponse.success("Deleted", null);
    }




}
