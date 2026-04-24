-- Add seen status to chat_messages
ALTER TABLE chat_messages ADD COLUMN seen BOOLEAN DEFAULT FALSE;
ALTER TABLE chat_messages ADD COLUMN seen_at TIMESTAMP NULL;
