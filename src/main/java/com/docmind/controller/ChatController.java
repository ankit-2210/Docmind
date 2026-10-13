package com.docmind.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(
        name = "Chat Management",
        description = "All chat related to api goes here"
)
public class ChatController {

    @GetMapping
    @Operation(
            summary = "Upload and index a document(PDF, DOCX, TEXT, MD, CSV)",
            description = "This api is used to upload and index documents files."
    )
    public ResponseEntity<String> uploadDocument(){
        return ResponseEntity.ok("Uploaded");
    }



}
