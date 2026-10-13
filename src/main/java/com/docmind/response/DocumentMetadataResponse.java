package com.docmind.response;

import com.docmind.utils.DocumentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DocumentMetadataResponse {
    private UUID id;

    private String filename;
    private String contentType;
    private Long fileSize;

    private Integer totalPages;
    private Integer totalChunks;
    private DocumentStatus status;
    private String errorMessage;

    private LocalDateTime createdAt;
    private  LocalDateTime updatedAt;


}
