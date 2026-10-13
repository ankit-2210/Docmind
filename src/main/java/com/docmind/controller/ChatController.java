package com.docmind.controller;

import com.docmind.dto.SearchResultDto;
import com.docmind.model.User;
import com.docmind.request.ChatRequest;
import com.docmind.request.SearchRequests;
import com.docmind.response.ApiResponse;
import com.docmind.response.ChatResponse;
import com.docmind.service.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import javax.naming.directory.SearchResult;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat")
@Tag(
        name = "Chat Management",
        description = "All chat related to api goes here"
)
public class ChatController {
    private final RagService ragService;


    @GetMapping
    @Operation(
            summary = "Upload and index a document(PDF, DOCX, TEXT, MD, CSV)",
            description = "This api is used to upload and index documents files."
    )
    public ResponseEntity<String> uploadDocument(){
        return ResponseEntity.ok("Uploaded");
    }


    @PostMapping("/query")
    @Operation(summary = "Ask a question against all documents or a specific document with citations")
    public ApiResponse<ChatResponse> askQuestion(@Valid @RequestBody ChatRequest request, Authentication authentication){
        User user = (User) authentication.getPrincipal();
        ChatResponse chatResponse = ragService.askQuestion(request, user);
        return ApiResponse.success(chatResponse);
    }

    @PostMapping(value = "/stream", produces = org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream real-time Q&A answer tokens via Server-Sent Events (SSE)")
    public ApiResponse<Flux<String>> streamQuestion(@Valid @RequestBody ChatRequest requestDto, Authentication authentication){
        User user = (User) authentication.getPrincipal();
        return ApiResponse.success(ragService.streamQuestionAnswer(requestDto, user));
    }

    @PostMapping("/search/similarity")
    @Operation(summary = "Perform semantic similarity search on stored document vectors")
    public ApiResponse<SearchResultDto> searchSimilar(@Valid @RequestBody SearchRequests request, Authentication authentication){
        User user = (User) authentication.getPrincipal();
        SearchResultDto results = ragService.searchSimilarChunks(request, user);
        return ApiResponse.success(results);
    }



}
