-- Thêm cột kiểm duyệt cho reviews
ALTER TABLE reviews ADD COLUMN approved BOOLEAN DEFAULT FALSE;
ALTER TABLE reviews ADD COLUMN moderation_status VARCHAR(50) DEFAULT 'PENDING';

-- Index cho cột mới
CREATE INDEX idx_reviews_approved ON reviews(approved);
CREATE INDEX idx_reviews_moderation_status ON reviews(moderation_status);
