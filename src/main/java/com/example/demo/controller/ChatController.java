package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.ChatMessage;
import com.example.demo.repository.ChatMessageRepository;
import com.example.demo.service.ChatService;
import com.example.demo.web.CurrentUserService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Controller
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private final ChatService chatService;
    private final CurrentUserService currentUserService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;

    public ChatController(ChatService chatService,
                         CurrentUserService currentUserService,
                         SimpMessagingTemplate messagingTemplate,
                         ChatMessageRepository chatMessageRepository) {
        this.chatService = chatService;
        this.currentUserService = currentUserService;
        this.messagingTemplate = messagingTemplate;
        this.chatMessageRepository = chatMessageRepository;
    }

    @MessageMapping("/chat/user")
    public void handleUserMessage(Message message) {
        try {
            // Sử dụng sender (username) làm sessionId để nhóm cuộc trò chuyện đúng
            String username = message.getSender();
            if (username == null || username.isEmpty()) {
                username = UUID.randomUUID().toString();
            }
            message.setSessionId(username);
            message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            message.setRole("USER");

            chatService.saveMessage(message);
            messagingTemplate.convertAndSend("/topic/messages", message);
            log.info("User message saved with session: {}, content: {}", username, message.getContent());
        } catch (Exception e) {
            log.error("Error handling user message", e);
        }
    }

    @MessageMapping("/chat/admin")
    public void handleAdminMessage(Message message) {
        try {
            message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            message.setRole("ADMIN");
            chatService.saveAdminMessage(message);

            // Gửi riêng cho user đích (sessionId chính là username của user)
            String targetUser = message.getSessionId();
            if (targetUser != null && !targetUser.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/messages/" + targetUser, message);
                log.info("Admin message sent to user {}: {}", targetUser, message.getContent());
            }

            // Vẫn broadcast cho admin dashboard
            messagingTemplate.convertAndSend("/topic/admin-messages", message);
        } catch (Exception e) {
            log.error("Error handling admin message", e);
        }
    }

    @GetMapping("/api/chat/history")
    @ResponseBody
    public ResponseEntity<?> getChatHistory(HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        List<ChatMessage> messages = chatService.getAllMessages();
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/api/chat/conversations")
    @ResponseBody
    public ResponseEntity<?> getConversations(HttpSession session) {
        try {
            currentUserService.requireAdmin(session);
            List<Map<String, Object>> conversations = chatService.getConversationsWithUnread();
            return ResponseEntity.ok(conversations);
        } catch (CurrentUserService.UnauthorizedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/chat/conversation/{sessionId}/seen")
    @ResponseBody
    public ResponseEntity<?> markConversationAsSeen(@PathVariable String sessionId, HttpSession session) {
        try {
            currentUserService.requireAdmin(session);
            // Mark all user messages as seen for this sender
            List<ChatMessage> messages = chatMessageRepository.findMessagesBySender(sessionId);
            for (ChatMessage msg : messages) {
                if (!msg.isSeen() && msg.getRole() == ChatMessage.ChatRole.USER) {
                    msg.setSeen(true);
                    chatMessageRepository.save(msg);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (CurrentUserService.UnauthorizedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/chat/conversation/{sessionId}")
    @ResponseBody
    public ResponseEntity<?> getConversation(@PathVariable String sessionId, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }
        // Find all messages with this sessionId (both user and admin messages)
        List<ChatMessage> userMessages = chatMessageRepository.findBySenderOrderByCreatedAtAsc(sessionId);
        List<ChatMessage> adminMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        // Combine and sort by createdAt
        Set<ChatMessage> allMessages = new HashSet<>(userMessages);
        allMessages.addAll(adminMessages);

        List<ChatMessage> sortedMessages = allMessages.stream()
            .sorted(Comparator.comparing(ChatMessage::getCreatedAt))
            .toList();

        return ResponseEntity.ok(sortedMessages);
    }

    @GetMapping("/api/chat/user/conversation")
    @ResponseBody
    public ResponseEntity<?> getUserConversation(@RequestParam(required = false) String sessionId, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        Account account = currentUserService.requireUser(session);

        // Sử dụng username trực tiếp làm sessionId
        if (sessionId == null || sessionId.isEmpty()) {
            sessionId = account.getUsername();
        }

        // Đánh dấu tin nhắn admin là đã xem khi user mở chat
        chatService.markAdminMessagesAsSeen(sessionId);

        List<ChatMessage> messages = chatService.getUserConversation(sessionId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/api/chat/send")
    @ResponseBody
    public ResponseEntity<?> sendApi(@RequestBody Message message, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Vui lòng đăng nhập"));
        }

        Account account = currentUserService.requireUser(session);
        message.setSender(account.getUsername());
        message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        message.setRole("USER");

        // Luôn dùng username làm sessionId để gom cuộc trò chuyện đúng
        if (message.getSessionId() == null || message.getSessionId().isEmpty()) {
            message.setSessionId(account.getUsername());
        }

        chatService.saveMessage(message);
        messagingTemplate.convertAndSend("/topic/messages", message);
        return ResponseEntity.ok(message);
    }

    @PostMapping("/api/chat/admin/send")
    @ResponseBody
    public ResponseEntity<?> sendAdminApi(@RequestBody Message message, HttpSession session) {
        try {
            Account account = currentUserService.requireAdmin(session);
            String sessionId = message.getSessionId();
            message.setSender("Admin");
            message.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            message.setRole("ADMIN");

            ChatMessage saved = chatService.saveAdminMessageWithSession(message, sessionId);

            // Gửi riêng cho user đích
            if (sessionId != null && !sessionId.isEmpty()) {
                messagingTemplate.convertAndSend("/topic/messages/" + sessionId, message);
            }

            return ResponseEntity.ok(saved);
        } catch (CurrentUserService.UnauthorizedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/chat/messages")
    @ResponseBody
    public ResponseEntity<?> clearMessages(HttpSession session) {
        try {
            currentUserService.requireAdmin(session);
            chatService.deleteAllMessages();
            return ResponseEntity.ok(Map.of("success", true));
        } catch (CurrentUserService.UnauthorizedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/chat/messages/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteMessage(@PathVariable Long id, HttpSession session) {
        try {
            currentUserService.requireAdmin(session);
            chatService.deleteMessage(id);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (CurrentUserService.UnauthorizedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/chat/conversation/{sessionId}")
    @ResponseBody
    public ResponseEntity<?> deleteConversation(@PathVariable String sessionId, HttpSession session) {
        try {
            currentUserService.requireAdmin(session);
            chatService.deleteConversation(sessionId);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (CurrentUserService.UnauthorizedException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/chat/check-login")
    @ResponseBody
    public Map<String, Object> checkLogin(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        if (session.getAttribute("userId") != null) {
            try {
                Account account = currentUserService.requireUser(session);
                response.put("loggedIn", true);
                response.put("username", account.getUsername());
                response.put("role", account.getRole().name());
            } catch (Exception e) {
                response.put("loggedIn", false);
            }
        } else {
            response.put("loggedIn", false);
        }
        return response;
    }

    public static class Message {
        private String sender;
        private String content;
        private String timestamp;
        private String role;
        private String sessionId;

        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    }
}
