package com.example.demo.repository;

import com.example.demo.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findAllByOrderByCreatedAtAsc();
    List<ChatMessage> findAllByOrderByCreatedAtDesc();
    List<ChatMessage> findBySenderOrderByCreatedAtAsc(String sender);
    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<ChatMessage> findBySessionIdOrderByCreatedAtDesc(String sessionId);
    List<ChatMessage> findBySenderOrderByCreatedAtDesc(String sender);

    // Find messages by sender (username) - groups conversations
    @Query("SELECT m FROM ChatMessage m WHERE m.sender = :sender ORDER BY m.createdAt ASC")
    List<ChatMessage> findMessagesBySender(@Param("sender") String sender);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.seen = true, m.seenAt = CURRENT_TIMESTAMP WHERE m.sessionId = :sessionId AND m.role = 'USER' AND m.seen = false")
    int markMessagesAsSeen(@Param("sessionId") String sessionId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.seen = true, m.seenAt = CURRENT_TIMESTAMP WHERE m.sessionId = :sessionId AND m.id <= :lastMessageId AND m.role = 'USER'")
    int markMessagesAsSeenUntil(@Param("sessionId") String sessionId, @Param("lastMessageId") Long lastMessageId);

    long countBySessionIdAndRoleAndSeenFalse(String sessionId, ChatMessage.ChatRole role);
}
