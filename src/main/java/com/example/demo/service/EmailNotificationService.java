package com.example.demo.service;

import com.example.demo.entity.ShopOrder;
import com.example.demo.entity.OrderLine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class EmailNotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    @Value("${spring.mail.username:noreply@shop.com}")
    private String fromEmail;

    @Value("${app.email.mock-mode:false}")
    private boolean mockMode;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Async
    public void sendOrderConfirmation(ShopOrder order, String username, String email) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("orderId", order.getId());
        variables.put("orderDate", DATE_FORMAT.format(order.getCreatedAt()));
        variables.put("paymentMethod", formatPaymentMethod(order.getPaymentMethod()));
        variables.put("totalAmount", order.getTotalAmount());
        variables.put("shopUrl", appBaseUrl + "/my-orders");
        variables.put("orderItems", buildOrderItems(order.getLines()));

        sendHtmlEmail(
            email,
            "Xác nhận đơn hàng #" + order.getId() + " - Genshin Cute Shop",
            "emails/order-created",
            variables
        );
    }

    @Async
    public void sendOrderPaidNotification(ShopOrder order, String username, String email) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("orderId", order.getId());
        variables.put("paidDate", order.getPaidAt() != null ? DATE_FORMAT.format(order.getPaidAt()) : "N/A");
        variables.put("paymentMethod", formatPaymentMethod(order.getPaymentMethod()));
        variables.put("totalAmount", order.getTotalAmount());
        variables.put("shopUrl", appBaseUrl + "/my-orders");
        variables.put("supportUrl", appBaseUrl + "/contact");
        variables.put("orderItems", buildOrderItemsWithCredentials(order.getLines()));

        sendHtmlEmail(
            email,
            "🎉 Thanh toán thành công - Đơn hàng #" + order.getId(),
            "emails/order-paid",
            variables
        );
    }

    @Async
    public void sendOrderRefundedNotification(ShopOrder order, String username, String email) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("orderId", order.getId());
        variables.put("refundAmount", order.getTotalAmount());
        variables.put("refundDate", DATE_FORMAT.format(new Date()));
        variables.put("shopUrl", appBaseUrl);
        variables.put("orderItems", buildOrderItems(order.getLines()));

        sendHtmlEmail(
            email,
            "💰 Hoàn tiền thành công - Đơn hàng #" + order.getId(),
            "emails/order-refunded",
            variables
        );
    }

    @Async
    public void sendWelcomeEmail(String username, String email, String createdAt) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("email", email);
        variables.put("createdAt", createdAt);
        variables.put("loginUrl", appBaseUrl + "/login");
        variables.put("shopUrl", appBaseUrl);

        sendHtmlEmail(
            email,
            "Chào mừng đến với Genshin Cute Shop! 🎮",
            "emails/welcome",
            variables
        );
    }

    @Async
    public void sendPasswordResetEmail(String username, String email, String resetToken, String ipAddress) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("resetToken", resetToken);
        variables.put("ipAddress", ipAddress != null ? ipAddress : "Không xác định");
        variables.put("resetUrl", appBaseUrl + "/reset-password?token=" + resetToken);

        sendHtmlEmail(
            email,
            "🔑 Yêu cầu đặt lại mật khẩu - Genshin Cute Shop",
            "emails/password-reset",
            variables
        );
    }

    @Async
    public void sendWithdrawalRequestEmail(String username, String email, long amount,
            String method, String bankName, String bankAccount) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("amount", amount);
        variables.put("withdrawalMethod", method);
        variables.put("bankName", bankName);
        variables.put("bankAccount", bankAccount);
        variables.put("requestDate", DATE_FORMAT.format(new Date()));
        variables.put("walletUrl", appBaseUrl + "/wallet");

        sendHtmlEmail(
            email,
            "📋 Xác nhận yêu cầu rút tiền - " + formatMoney(amount),
            "emails/withdrawal-request",
            variables
        );
    }

    @Async
    public void sendWithdrawalCompleteEmail(String username, String email, long amount,
            String method, String bankName, String bankAccount) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("amount", amount);
        variables.put("withdrawalMethod", method);
        variables.put("bankName", bankName);
        variables.put("bankAccount", bankAccount);
        variables.put("processedAt", DATE_FORMAT.format(new Date()));
        variables.put("walletUrl", appBaseUrl + "/wallet");

        sendHtmlEmail(
            email,
            "✅ Yêu cầu rút tiền đã hoàn tất - " + formatMoney(amount),
            "emails/withdrawal-complete",
            variables
        );
    }

    @Async
    public void sendDepositConfirmationEmail(String username, String email, long amount,
            String paymentMethod, String transactionId, String transferContent,
            String bankAccount, String bankName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("amount", amount);
        variables.put("paymentMethod", paymentMethod);
        variables.put("transactionId", transactionId);
        variables.put("depositDate", DATE_FORMAT.format(new Date()));
        variables.put("transferContent", transferContent);
        variables.put("bankAccount", bankAccount);
        variables.put("bankName", bankName);
        variables.put("walletUrl", appBaseUrl + "/wallet");

        sendHtmlEmail(
            email,
            "💳 Xác nhận yêu cầu nạp tiền - " + formatMoney(amount),
            "emails/deposit-confirmation",
            variables
        );
    }

    @Async
    public void sendAccountApprovedEmail(String username, String email, String productName) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("productName", productName);
        variables.put("shopUrl", appBaseUrl + "/my-orders");

        sendHtmlEmail(
            email,
            "✅ Tài khoản của bạn đã được duyệt - " + productName,
            "emails/welcome",
            variables
        );
    }

    @Async
    public void sendAccountRejectedEmail(String username, String email, String productName, String reason) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("productName", productName);
        variables.put("reason", reason);
        variables.put("shopUrl", appBaseUrl);

        sendHtmlEmail(
            email,
            "❌ Tài khoản không được duyệt - " + productName,
            "emails/welcome",
            variables
        );
    }

    private void sendHtmlEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (mockMode) {
            System.out.println("\n========== MOCK EMAIL ==========");
            System.out.println("TO: " + to);
            System.out.println("SUBJECT: " + subject);
            System.out.println("TEMPLATE: " + templateName);
            System.out.println("VARIABLES: " + variables);
            System.out.println("================================\n");
            return;
        }

        try {
            Context context = new Context();
            variables.put("subject", subject);
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send email to " + to + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    private List<OrderItemDto> buildOrderItems(List<OrderLine> lines) {
        List<OrderItemDto> items = new ArrayList<>();
        for (OrderLine line : lines) {
            OrderItemDto dto = new OrderItemDto();
            dto.setName(line.getProductNameSnapshot());
            dto.setPrice(line.getUnitPrice());
            items.add(dto);
        }
        return items;
    }

    private List<OrderItemDto> buildOrderItemsWithCredentials(List<OrderLine> lines) {
        List<OrderItemDto> items = new ArrayList<>();
        for (OrderLine line : lines) {
            OrderItemDto dto = new OrderItemDto();
            dto.setName(line.getProductNameSnapshot());
            dto.setPrice(line.getUnitPrice());
            dto.setUsername(""); // Will be parsed from credentials
            dto.setPassword(""); // Will be parsed from credentials

            String creds = line.getDeliveredCredentials();
            if (creds != null && !creds.isBlank()) {
                String[] parts = creds.split("\\|");
                if (parts.length >= 2) {
                    dto.setUsername(parts[0].replace("user:", "").trim());
                    dto.setPassword(parts[1].replace("pass:", "").trim());
                } else {
                    dto.setUsername(creds);
                    dto.setPassword("Đã gửi kèm");
                }
            }
            items.add(dto);
        }
        return items;
    }

    private String formatPaymentMethod(String method) {
        if (method == null) return "Không xác định";
        return switch (method) {
            case "WALLET" -> "Ví điện tử";
            case "BANK" -> "Chuyển khoản ngân hàng";
            case "VNPAY" -> "VNPay";
            case "MOMO" -> "MoMo";
            default -> method;
        };
    }

    private String formatMoney(long amount) {
        return String.format("%,dđ", amount).replace(",", ".");
    }
}
