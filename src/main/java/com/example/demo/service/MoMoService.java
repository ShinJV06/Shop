package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class MoMoService {

    @Value("${momo.partner.code:}")
    private String partnerCode;

    @Value("${momo.access.key:}")
    private String accessKey;

    @Value("${momo.secret.key:}")
    private String secretKey;

    @Value("${momo.payment.url:https://test-payment.momo.vn/v2/gateway/api/create}")
    private String momoPaymentUrl;

    @Value("${momo.return.url:http://localhost:8080/payment/momo/return}")
    private String momoReturnUrl;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String createPaymentUrl(Long orderId, long amount, String requestId) {
        String orderInfo = "Thanh toan don hang #" + orderId;
        String requestType = "captureWallet";
        String notifyUrl = appBaseUrl + "/payment/momo/ipn";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("partnerCode", partnerCode);
        params.put("partnerName", "GameShop");
        params.put("storeId", partnerCode);
        params.put("requestType", requestType);
        params.put("ipnUrl", notifyUrl);
        params.put("redirectUrl", momoReturnUrl + "?orderId=" + orderId);
        params.put("orderId", orderId + "_" + System.currentTimeMillis());
        params.put("amount", String.valueOf(amount));
        params.put("lang", "vi");
        params.put("orderInfo", orderInfo);
        params.put("requestId", requestId);

        String rawData = buildRawData(params);
        String signature = hmacSHA256(rawData, secretKey);
        params.put("signature", signature);

        return momoPaymentUrl + "?partnerCode=" + partnerCode + 
               "&accessKey=" + accessKey + 
               "&requestId=" + requestId +
               "&amount=" + amount +
               "&orderId=" + params.get("orderId") +
               "&orderInfo=" + URLEncoder.encode(orderInfo, StandardCharsets.UTF_8) +
               "&returnUrl=" + URLEncoder.encode(momoReturnUrl + "?orderId=" + orderId, StandardCharsets.UTF_8) +
               "&notifyUrl=" + URLEncoder.encode(notifyUrl, StandardCharsets.UTF_8) +
               "&requestType=" + requestType +
               "&signature=" + signature;
    }

    public Map<String, String> createPaymentRequest(Long orderId, long amount, String requestId) {
        String orderInfo = "Thanh toan don hang #" + orderId;
        String requestType = "captureWallet";
        String notifyUrl = appBaseUrl + "/payment/momo/ipn";

        Map<String, String> params = new LinkedHashMap<>();
        params.put("partnerCode", partnerCode);
        params.put("partnerName", "GameShop");
        params.put("storeId", partnerCode);
        params.put("requestType", requestType);
        params.put("ipnUrl", notifyUrl);
        params.put("redirectUrl", momoReturnUrl + "?orderId=" + orderId);
        params.put("orderId", orderId + "_" + System.currentTimeMillis());
        params.put("amount", String.valueOf(amount));
        params.put("lang", "vi");
        params.put("orderInfo", orderInfo);
        params.put("requestId", requestId);

        String rawData = buildRawData(params);
        String signature = hmacSHA256(rawData, secretKey);
        params.put("signature", signature);

        return params;
    }

    public boolean validateSignature(Map<String, Object> params) {
        if (!params.containsKey("signature")) return false;
        
        String receivedSignature = params.get("signature").toString();
        Map<String, String> signParams = new HashMap<>();
        params.forEach((key, value) -> {
            if (value != null) signParams.put(key, value.toString());
        });
        signParams.remove("signature");
        
        String rawData = buildRawData(signParams);
        String calculatedSignature = hmacSHA256(rawData, secretKey);
        
        return calculatedSignature.equalsIgnoreCase(receivedSignature);
    }

    public Long extractOrderId(String orderId) {
        if (orderId == null || !orderId.contains("_")) return null;
        try {
            return Long.parseLong(orderId.split("_")[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String buildRawData(Map<String, ?> params) {
        StringBuilder sb = new StringBuilder();
        List<String> keys = new ArrayList<>(params.keySet());
        Collections.sort(keys);
        
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Object value = params.get(key);
            if (value != null && !value.toString().isEmpty()) {
                sb.append(key).append("=").append(value.toString());
                if (i < keys.size() - 1) sb.append("&");
            }
        }
        return sb.toString();
    }

    private String hmacSHA256(String data, String key) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi tạo HMAC SHA256", e);
        }
    }
}
