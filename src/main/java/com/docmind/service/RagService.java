package com.docmind.service;

import com.docmind.dto.SearchResultDto;
import com.docmind.model.User;
import com.docmind.request.ChatRequest;
import com.docmind.request.SearchRequests;
import com.docmind.response.ChatResponse;
import reactor.core.publisher.Flux;

public interface RagService {
    ChatResponse askQuestion(ChatRequest request, User user);

    Flux<String> streamQuestionAnswer(ChatRequest request, User user);

    SearchResultDto searchSimilarChunks(SearchRequests request, User user);

}
