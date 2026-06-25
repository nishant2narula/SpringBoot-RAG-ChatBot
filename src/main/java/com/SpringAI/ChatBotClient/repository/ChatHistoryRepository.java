package com.SpringAI.ChatBotClient.repository;

import com.SpringAI.ChatBotClient.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, Long> {

    // Get all history for a specific session
    List<ChatHistory> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    // Get all history grouped by latest first
    List<ChatHistory> findAllByOrderByCreatedAtDesc();

    // Get recent history for a session (last 10)
    List<ChatHistory> findTop10BySessionIdOrderByCreatedAtDesc(String sessionId);

    // Delete history for a specific session
    void deleteBySessionId(String sessionId);

}
