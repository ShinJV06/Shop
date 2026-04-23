-- Tạo bảng reviews để lưu đánh giá của khách hàng
CREATE TABLE IF NOT EXISTS reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    user_avatar VARCHAR(500),
    rating INT NOT NULL,
    comment VARCHAR(1000),
    product_slug VARCHAR(255),
    product_name VARCHAR(255),
    image_paths VARCHAR(1000),
    created_at DATETIME,
    INDEX idx_created_at (created_at),
    INDEX idx_product_slug (product_slug)
);
