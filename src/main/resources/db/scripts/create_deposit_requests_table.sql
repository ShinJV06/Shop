-- Script tạo bảng deposit_requests cho hệ thống nạp tiền tự động VNPay/MoMo
-- Chạy script này trên MySQL database của bạn

CREATE TABLE IF NOT EXISTS deposit_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL COMMENT 'VNPAY, MOMO',
    transaction_id VARCHAR(100),
    order_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã đơn nạp tiền duy nhất (NAP...)',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED, CANCELLED',
    qr_data TEXT COMMENT 'Dữ liệu QR code base64',
    payment_url VARCHAR(500) COMMENT 'URL thanh toán VNPay/MoMo',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME,
    expires_at DATETIME COMMENT 'Thời hạn thanh toán',
    completed_at DATETIME COMMENT 'Thời gian hoàn thành',
    
    INDEX idx_user_id (user_id),
    INDEX idx_order_id (order_id),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comment
COMMENT ON TABLE deposit_requests IS 'Bảng lưu yêu cầu nạp tiền qua VNPay/MoMo';
COMMENT ON COLUMN deposit_requests.payment_method IS 'Phương thức thanh toán: VNPAY, MOMO';
COMMENT ON COLUMN deposit_requests.order_id IS 'Mã đơn nạp tiền format: NAP + timestamp + random (VD: NAP17123456789012340001)';
COMMENT ON COLUMN deposit_requests.status IS 'Trạng thái: PENDING(chờ), PROCESSING(đang xử lý), COMPLETED(hoàn thành), FAILED(thất bại), EXPIRED(hết hạn), CANCELLED(đã hủy)';
