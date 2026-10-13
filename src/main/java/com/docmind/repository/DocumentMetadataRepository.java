package com.docmind.repository;

import com.docmind.model.DocumentMetadata;
import com.docmind.model.User;
import com.docmind.utils.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, UUID> {

    List<DocumentMetadata> findByStatus(DocumentStatus status);
    List<DocumentMetadata> findAllByOrderByCreatedAtDesc();

    List<DocumentMetadata> findByUserOrderByCreatedAtDesc(User user);
    Optional<DocumentMetadata> findByIdAndUser(UUID id, User user);


}
