package com.SpringAI.ChatBotClient.repository;

import com.SpringAI.ChatBotClient.entity.IngestedDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IngestedDocumentRepository extends JpaRepository<IngestedDocument, Long> {

    boolean existsByFileName(String fileName);
    boolean existsByFileHash(String fileHash);
    Optional<IngestedDocument> findByFileName(String fileName);

}
