package com.docmind.service;

import com.docmind.model.DocumentMetadata;
import org.springframework.ai.document.Document;

import java.util.*;

public interface DocumentIngestionService {
    int ingest(DocumentMetadata metadata, List<Document> parsedDocs);


}
