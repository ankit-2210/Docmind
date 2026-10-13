package com.docmind.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRequest {
    @NotBlank(message = "Question cannot be empty")
    private String question;

    private UUID documentId;

    private Integer topK;
    private Double minSimilarity;

    private String conversationId;



}
