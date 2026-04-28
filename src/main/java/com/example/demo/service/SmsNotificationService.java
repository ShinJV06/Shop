package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class SmsNotificationService {

    @Value("${sms.enabled:false}")
    private boolean enabled;

    @Value("${sms.provider:mock}")
    private String provider;

    @Value("${sms.brand-name:GameShop}")
    private String brandName;

    @Value("${sms.api-key:}")
    private String apiKey;

    @Value("${sms.secret-key:}")
    private String secretKey;

    public boolean sendOtp(String phone, String otp) {
        String message = "Ma xac minh Genshin Cute Shop: " + otp + ". Het han sau 5 phut.";
        return sendSms(phone, message);
    }

    public boolean sendOrderOtp(String phone, String orderId, String otp) {
        String message = "Xac nhan thanh toan don #" + orderId + ": " + otp + ". Ma het han sau 5 phut.";
        return sendSms(phone, message);
    }

    public boolean sendOrderCreatedSms(String phone, String orderId, String amount) {
        String message = "Don hang #" + orderId + " da tao thanh cong. So tien: " + amount + "d. Vui long thanh toan trong 24h.";
        return sendSms(phone, message);
    }

    public boolean sendOrderPaidSms(String phone, String orderId) {
        String message = "Don hang #" + orderId + " da thanh toan thanh cong! Thong tin tai khoan da duoc gui qua email.";
        return sendSms(phone, message);
    }

    public boolean sendRefundSms(String phone, String orderId, String amount) {
        String message = "Yeu cau hoan tien don #" + orderId + " da duoc xu ly. So tien hoan: " + amount + "d vao vi cua ban.";
        return sendSms(phone, message);
    }

    public boolean sendWithdrawalSms(String phone, String amount) {
        String message = "Yeu cau rut tien " + amount + "d da duoc xu ly thanh cong. Tien da chuyen vao tai khoan cua ban.";
        return sendSms(phone, message);
    }

    public boolean sendDepositSms(String phone, String amount) {
        String message = "Nap tien " + amount + "d thanh cong! So du vi hien tai da duoc cap nhat.";
        return sendSms(phone, message);
    }

    public boolean sendPasswordResetSms(String phone, String code) {
        String message = "Ma dat lai mat khau Genshin Cute Shop: " + code + ". Het han sau 15 phut.";
        return sendSms(phone, message);
    }

    public boolean sendAccountDeliveredSms(String phone, String gameName) {
        String message = "Tai khoan " + gameName + " da duoc giao! Vui long kiem tra email de nhan thong tin dang nhap.";
        return sendSms(phone, message);
    }

    private boolean sendSms(String phone, String message) {
        try {
            String formattedPhone = formatPhoneNumber(phone);
            if (formattedPhone == null) {
                System.err.println("[SMS] Invalid phone number: " + phone);
                return false;
            }

            return switch (provider.toLowerCase()) {
                case "vietguys" -> sendViaVietGuys(formattedPhone, message);
                case "esms" -> sendViaESms(formattedPhone, message);
                case "gatewayapi" -> sendViaGatewayApi(formattedPhone, message);
                default -> {
                    if (enabled) {
                        System.out.println("[SMS] To: " + formattedPhone + ", Message: " + message);
                    } else {
                        System.out.println("[SMS] Disabled - Would send to " + formattedPhone + ": " + message);
                    }
                    yield true;
                }
            };
        } catch (Exception e) {
            System.err.println("[SMS] Failed to send SMS: " + e.getMessage());
            return false;
        }
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        phone = phone.replaceAll("[\\s\\-\\(\\)]", "");
        if (phone.startsWith("0")) {
            phone = "84" + phone.substring(1);
        }
        if (!phone.matches("^84[0-9]{9,10}$")) {
            return null;
        }
        return phone;
    }

    private boolean sendViaVietGuys(String phone, String message) {
        // Implementation for VietGuys SMS Gateway
        // Register at: https://vietguys.vn/
        // API documentation: https://vietguys.vn/developers/sms-api
        System.out.println("[VietGuys SMS] To: " + phone + ", Message: " + message);
        return true;
    }

    private boolean sendViaESms(String phone, String message) {
        // Implementation for eSMS.vn
        // Register at: https://esms.vn/
        // API documentation: https://esms.vn/sms-api-docs
        System.out.println("[eSMS] To: " + phone + ", Message: " + message);
        return true;
    }

    private boolean sendViaGatewayApi(String phone, String message) {
        // Implementation for GatewayAPI
        // Register at: https://gatewayapi.com/
        // API documentation: https://gatewayapi.com/docs/
        System.out.println("[GatewayAPI] To: " + phone + ", Message: " + message);
        return true;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
