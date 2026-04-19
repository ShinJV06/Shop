-- Sửa cột role nếu DB cũ là ENUM/VARCHAR quá ngắn (lỗi Data truncated khi chọn CTV/BANNED).
-- Bỏ qua lỗi nếu cột đã đúng kiểu (một số phiên bản MySQL không hỗ trợ IF EXISTS cho MODIFY).
ALTER TABLE account MODIFY COLUMN role VARCHAR(32) NOT NULL;
