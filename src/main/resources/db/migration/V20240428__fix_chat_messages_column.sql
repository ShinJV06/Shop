-- Fix chat_messages table - ensure correct column names
-- This migration handles the case where the table might have been created with wrong column names

-- Check if table exists and alter if needed
SET @column_exists = (
    SELECT COUNT(*) 
    FROM INFORMATION_SCHEMA.COLUMNS 
    WHERE TABLE_SCHEMA = 'shop' 
    AND TABLE_NAME = 'chat_messages' 
    AND COLUMN_NAME = 'role'
);

-- If 'role' column exists, rename it to 'chat_role'
SET @sql = IF(@column_exists > 0, 
    'ALTER TABLE chat_messages CHANGE COLUMN role chat_role VARCHAR(50) NOT NULL DEFAULT "USER"', 
    'SELECT "Column chat_role already exists or table not created yet"');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
