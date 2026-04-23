-- Create reviews table
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
    INDEX idx_reviews_product_slug (product_slug),
    INDEX idx_reviews_created_at (created_at)
);
