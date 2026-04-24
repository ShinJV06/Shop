package com.example.demo.service;

import com.example.demo.controller.ChatController.Message;
import com.example.demo.entity.ChatMessage;
import com.example.demo.repository.ChatMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public List<ChatMessage> getAllMessages() {
        return chatMessageRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional
    public ChatMessage saveMessage(Message message) {
        try {
            ChatMessage chatMessage = new ChatMessage(
                message.getSender(),
                message.getContent(),
                message.getTimestamp(),
                ChatMessage.ChatRole.USER,
                message.getSessionId()
            );
            ChatMessage saved = chatMessageRepository.save(chatMessage);
            log.info("Message saved: id={}, sender={}, sessionId={}", saved.getId(), saved.getSender(), saved.getSessionId());
            return saved;
        } catch (Exception e) {
            log.error("Error saving user message", e);
            throw e;
        }
    }

    @Transactional
    public ChatMessage saveAdminMessage(Message message) {
        try {
            ChatMessage chatMessage = new ChatMessage(
                message.getSender(),
                message.getContent(),
                message.getTimestamp(),
                ChatMessage.ChatRole.ADMIN,
                message.getSessionId()
            );
            ChatMessage saved = chatMessageRepository.save(chatMessage);
            log.info("Admin message saved: id={}, sender={}, sessionId={}", saved.getId(), saved.getSender(), saved.getSessionId());
            return saved;
        } catch (Exception e) {
            log.error("Error saving admin message", e);
            throw e;
        }
    }

    @Transactional
    public ChatMessage saveAdminMessageWithSession(Message message, String sessionId) {
        try {
            ChatMessage chatMessage = new ChatMessage(
                message.getSender(),
                message.getContent(),
                message.getTimestamp(),
                ChatMessage.ChatRole.ADMIN,
                sessionId
            );
            ChatMessage saved = chatMessageRepository.save(chatMessage);
            log.info("Admin message saved with session: id={}, sender={}, sessionId={}", saved.getId(), saved.getSender(), saved.getSessionId());
            return saved;
        } catch (Exception e) {
            log.error("Error saving admin message", e);
            throw e;
        }
    }

    public List<ChatMessage> getConversationBySessionId(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public List<ChatMessage> getUserConversation(String sessionId) {
        // Lấy tin nhắn của user (sender = sessionId)
        List<ChatMessage> userMessages = chatMessageRepository.findBySenderOrderByCreatedAtAsc(sessionId);
        // Lấy tin nhắn admin gửi cho user này (sessionId = sessionId, role = ADMIN)
        List<ChatMessage> adminMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
            .stream()
            .filter(m -> m.getRole() == ChatMessage.ChatRole.ADMIN)
            .toList();
        
        // Gộp và sắp xếp
        Set<ChatMessage> all = new HashSet<>(userMessages);
        all.addAll(adminMessages);
        return all.stream().sorted(Comparator.comparing(ChatMessage::getCreatedAt)).toList();
    }

    public List<Map<String, Object>> getConversations() {
        List<ChatMessage> allMessages = chatMessageRepository.findAllByOrderByCreatedAtDesc();
        
        Map<String, Map<String, Object>> conversationMap = new LinkedHashMap<>();
        
        for (ChatMessage msg : allMessages) {
            String key = msg.getSessionId();
            if (key == null || key.isEmpty()) {
                key = msg.getSender();
            }
            
            if (!conversationMap.containsKey(key)) {
                Map<String, Object> conv = new HashMap<>();
                conv.put("sessionId", key);
                conv.put("user", msg.getSender());
                conv.put("lastMessage", msg.getContent());
                conv.put("lastTime", msg.getTimestamp());
                conv.put("lastTimeFull", msg.getCreatedAt().toString());
                conv.put("role", msg.getRole().name());
                conv.put("unread", 0);
                conversationMap.put(key, conv);
            } else {
                Map<String, Object> conv = conversationMap.get(key);
                conv.put("lastMessage", msg.getContent());
                conv.put("lastTime", msg.getTimestamp());
                conv.put("lastTimeFull", msg.getCreatedAt().toString());
            }
        }
        
        List<Map<String, Object>> conversations = new ArrayList<>(conversationMap.values());
        conversations.sort((a, b) -> {
            String timeA = (String) a.get("lastTimeFull");
            String timeB = (String) b.get("lastTimeFull");
            return timeB.compareTo(timeA);
        });
        
        return conversations;
    }

    @Transactional
    public void clearMessages() {
        chatMessageRepository.deleteAll();
    }

    @Transactional
    public void deleteMessage(Long id) {
        chatMessageRepository.deleteById(id);
    }

    @Transactional
    public void markAdminMessagesAsSeen(String username) {
        // Đánh dấu tất cả tin nhắn admin gửi cho user này là đã xem
        List<ChatMessage> adminMessages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(username)
            .stream()
            .filter(m -> m.getRole() == ChatMessage.ChatRole.ADMIN && !m.isSeen())
            .toList();

        for (ChatMessage msg : adminMessages) {
            msg.setSeen(true);
            chatMessageRepository.save(msg);
        }
    }

    @Transactional
    public void deleteAllMessages() {
        chatMessageRepository.deleteAll();
    }

    @Transactional
    public void deleteConversation(String sessionId) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
        chatMessageRepository.deleteAll(messages);
    }

    @Transactional
    public void markAsSeen(String sessionId) {
        chatMessageRepository.markMessagesAsSeen(sessionId);
    }

    public long getUnreadCount(String sessionId, ChatMessage.ChatRole role) {
        return chatMessageRepository.countBySessionIdAndRoleAndSeenFalse(sessionId, role);
    }

    public List<Map<String, Object>> getConversationsWithUnread() {
        List<ChatMessage> allMessages = chatMessageRepository.findAllByOrderByCreatedAtDesc();

        // Group by username - USER messages use sender, ADMIN messages use sessionId (which is username)
        Map<String, Map<String, Object>> conversationMap = new LinkedHashMap<>();

        for (ChatMessage msg : allMessages) {
            // Key luôn là username để gom đúng cuộc trò chuyện
            String key;
            if (msg.getRole() == ChatMessage.ChatRole.USER) {
                // Tin nhắn user → dùng sender (username)
                key = msg.getSender();
            } else {
                // Tin nhắn admin → dùng sessionId (username của user)
                key = msg.getSessionId();
            }

            if (key == null || key.isEmpty()) {
                continue; // Bỏ qua tin nhắn không có key
            }

            if (!conversationMap.containsKey(key)) {
                Map<String, Object> conv = new HashMap<>();
                conv.put("sessionId", key);
                conv.put("user", key); // Username hiển thị
                conv.put("lastMessage", msg.getContent());
                conv.put("lastTime", msg.getTimestamp());
                conv.put("lastTimeFull", msg.getCreatedAt().toString());
                conv.put("role", msg.getRole().name());
                conv.put("seen", msg.isSeen());
                conv.put("unread", msg.isSeen() ? 0 : 1);
                conv.put("lastMessageSeen", msg.isSeen());
                conversationMap.put(key, conv);
            } else {
                Map<String, Object> conv = conversationMap.get(key);
                conv.put("lastMessage", msg.getContent());
                conv.put("lastTime", msg.getTimestamp());
                conv.put("lastTimeFull", msg.getCreatedAt().toString());
                if (!msg.isSeen()) {
                    int currentUnread = (int) conv.getOrDefault("unread", 0);
                    conv.put("unread", currentUnread + 1);
                }
                conv.put("lastMessageSeen", msg.isSeen());
            }
        }

        List<Map<String, Object>> conversations = new ArrayList<>(conversationMap.values());
        conversations.sort((a, b) -> {
            String timeA = (String) a.get("lastTimeFull");
            String timeB = (String) b.get("lastTimeFull");
            return timeB.compareTo(timeA);
        });

        return conversations;
    }
}
