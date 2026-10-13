package com.docmind.response;

import com.docmind.dto.CitationDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatResponse {
    private String answer;
    private String conversationId;

    private List<CitationDto> citations;
    private Long responseTimeMs;


}
