package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false)
    private String timestamp;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "chat_role", nullable = false)
    private ChatRole role;

    @Column(name = "session_id")
    private String sessionId;

    @Column(name = "seen", nullable = false)
    private boolean seen = false;

    @Column(name = "seen_at")
    private LocalDateTime seenAt;

    public enum ChatRole {
        USER, ADMIN
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public ChatMessage() {}

    public ChatMessage(String sender, String content, String timestamp, ChatRole role) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.role = role;
    }

    public ChatMessage(String sender, String content, String timestamp, ChatRole role, String sessionId) {
        this.sender = sender;
        this.content = content;
        this.timestamp = timestamp;
        this.role = role;
        this.sessionId = sessionId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public ChatRole getRole() { return role; }
    public void setRole(ChatRole role) { this.role = role; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public boolean isSeen() { return seen; }
    public void setSeen(boolean seen) { this.seen = seen; }

    public LocalDateTime getSeenAt() { return seenAt; }
    public void setSeenAt(LocalDateTime seenAt) { this.seenAt = seenAt; }
}
