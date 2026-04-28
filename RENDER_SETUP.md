# HƯỚNG DẪN CẤU HÌNH RENDER

## 1. Tạo PostgreSQL Database (nếu chưa có)

1. Vào https://dashboard.render.com
2. Click **New +** → **PostgreSQL**
3. Điền:
   - Name: `shop-db`
   - Database: `shop`
   - User: `postgres` (hoặc để mặc định)
   - Region: Singapore
   - Instance Type: **Free**
4. Click **Create Database**
5. Đợi ~2 phút → Vào **Connect** → Copy **Internal Connection URL**

## 2. Cấu hình Web Service

1. Vào Web Service của bạn
2. Click **Environment**
3. Thêm từng biến bên dưới:

### BẮT BUỘC - Database:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `production` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://dpg-xxxxx-xxxxx:5432/shop` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `password_ban_dat_khi_tao_postgresql` |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` |
| `SPRING_JPA_DATABASE_PLATFORM` | `org.hibernate.dialect.PostgreSQLDialect` |
| `SERVER_PORT` | `10000` |

### Quan trọng - OAuth2 Google:

| Key | Value |
|-----|-------|
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID` | `386583134998-9g4fme7ausir5aed8lii0rfrnt2td715.apps.googleusercontent.com` |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` | `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` |

### Tùy chọn - URL trả về thanh toán:

| Key | Value |
|-----|-------|
| `APP_BASE_URL` | `https://shop-xxxx.onrender.com` |

## 3. Lấy đúng Internal URL

Sau khi tạo PostgreSQL, vào **Connect** → Copy URL dạng:

```
postgres://postgres:password@host:5432/shop
```

Chuyển thành JDBC URL:
```
jdbc:postgresql://host:5432/shop
```

- Host: phần sau `@` trước `:5432`
- Password: phần giữa `:` và `@`

## 4. Deploy

1. Sau khi thêm hết Environment Variables
2. Click **Save Changes**
3. Click **Manual Deploy** → **Deploy latest commit**

## 5. Sau khi deploy thành công

1. Vào **Logs** của Web Service
2. Copy URL website (VD: `https://shop-xxxx.onrender.com`)
3. Cập nhật:
   - `APP_BASE_URL` = URL đó
   - `Vnpay_RETURN_URL` = `https://shop-xxxx.onrender.com/payment/vnpay/return`
   - `MOMO_RETURN_URL` = `https://shop-xxxx.onrender.com/payment/momo/return`
4. Redeploy lại
