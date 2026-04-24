-- Create chat_messages table with session_id support
CREATE TABLE IF NOT EXISTS chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender VARCHAR(255) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    timestamp VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    chat_role VARCHAR(50) NOT NULL DEFAULT 'USER',
    session_id VARCHAR(255) DEFAULT NULL
);

CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender);
CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
