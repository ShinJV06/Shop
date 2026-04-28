# Hướng dẫn Deploy lên Render

## Yêu cầu

- Tài khoản Render (đăng ký free tại https://render.com)
- GitHub repository chứa source code
- PostgreSQL database (Render cung cấp free)

## Các bước thực hiện

### 1. Chuẩn bị Database

1. Đăng nhập Render Dashboard
2. Click **New +** → **PostgreSQL**
3. Điền thông tin:
   - Name: `demo-db`
   - Database: `demo`
   - User: `demo_user`
4. Click **Create Database**
5. Sau khi tạo xong, copy các giá trị:
   - `Internal Database URL` (sẽ dùng cho SPRING_DATASOURCE_URL)
   - Username và Password

### 2. Cấu hình trên Render

#### Cách 1: Deploy tự động qua GitHub

1. Click **New +** → **Web Service**
2. Kết nối GitHub repository của bạn
3. Cấu hình:
   - Name: `demo-app`
   - Region: Singapore
   - Branch: `main`
   - Root Directory: (để trống)
   - Runtime: **Java**
   - Build Command: `./mvnw clean package -DskipTests`
   - Start Command: `java -jar target/demo-0.0.1-SNAPSHOT.jar`
   - Plan: **Free**

4. Click **Advanced** → **Add Environment Variable** và thêm:

```
SPRING_PROFILES_ACTIVE = production
SERVER_PORT = 10000
SPRING_DATASOURCE_URL = jdbc:postgresql://host:5432/database
SPRING_DATASOURCE_USERNAME = your_username
SPRING_DATASOURCE_PASSWORD = your_password
SPRING_JPA_DATABASE_PLATFORM = org.hibernate.dialect.PostgreSQLDialect
SPRING_JPA_HIBERNATE_DDL_AUTO = update
APP_BASE_URL = https://your-app-name.onrender.com
```

5. Click **Create Web Service**

#### Cách 2: Dùng render.yaml (Blueprint)

1. Push file `render.yaml` đã tạo lên GitHub
2. Render sẽ tự động detect và deploy
3. Cần set các Environment Variables trên Render Dashboard (vì `sync: false`)

### 3. Cập nhật OAuth2 Google

1. Vào Google Cloud Console
2. Chọn project của bạn
3. Vào **APIs & Services** → **Credentials**
4. Click vào OAuth 2.0 Client ID
5. Trong phần **Authorized redirect URIs**, thêm:
   ```
   https://your-app-name.onrender.com/login/oauth2/code/google
   ```
6. Lấy `Client ID` và `Client Secret` mới
7. Thêm vào Render Environment Variables:
   ```
   SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID = your_client_id
   SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET = your_client_secret
   ```

### 4. Cập nhật URL sau khi deploy

Sau khi app được deploy thành công, update các URL:

```
APP_BASE_URL = https://demo-app.onrender.com
Vnpay_RETURN_URL = https://demo-app.onrender.com/payment/vnpay/return
MOMO_RETURN_URL = https://demo-app.onrender.com/payment/momo/return
```

### 5. Kiểm tra

- Truy cập `https://your-app-name.onrender.com`
- Kiểm tra các chức năng:
  - [ ] Đăng nhập/Đăng ký
  - [ ] Đăng nhập Google
  - [ ] Upload file
  - [ ] Thanh toán VNPay/MoMo

## Xử lý lỗi thường gặp

### Lỗi Database Connection
```
Could not connect to address=(host=...)
```
→ Kiểm tra SPRING_DATASOURCE_URL, username, password

### Lỗi Port
```
Port already in use
```
→ Đảm bảo SERVER_PORT = 10000

### Lỗi Build
```
./mvnw: Permission denied
```
→ Chạy: `chmod +x mvnw`

### Lỗi 500 khi upload file
→ Cần cấu hình Cloud Storage (AWS S3, Cloudinary) thay vì lưu local

## Lưu ý bảo mật

- KHÔNG commit file `application.properties` với credentials thật
- Sử dụng Environment Variables cho tất cả secrets
- Xóa các credentials cũ trong code trước khi push

## Giới hạn Free Tier Render

- 750 giờ/tháng (đủ cho 1 app chạy 24/7)
- App ngủ sau 15 phút không hoạt động
- Không có persistent disk (cần dùng cloud storage cho files)
- PostgreSQL free: 1GB storage
