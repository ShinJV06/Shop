-- Add session_id column to chat_messages table
ALTER TABLE chat_messages 
ADD COLUMN session_id VARCHAR(255) DEFAULT NULL;

-- Create index for faster session queries
CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
