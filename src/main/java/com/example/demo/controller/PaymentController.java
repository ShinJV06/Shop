package com.example.demo.controller;

import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.ShopOrder;
import com.example.demo.repository.ShopOrderRepository;
import com.example.demo.service.MoMoService;
import com.example.demo.service.NotificationService;
import com.example.demo.service.OrderFlowService;
import com.example.demo.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Autowired
    private VNPayService vnpayService;

    @Autowired
    private MoMoService moMoService;

    @Autowired
    private OrderFlowService orderFlowService;

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/create-vnpay")
    public ResponseEntity<?> createVNPayPayment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long orderId = Long.parseLong(body.get("orderId").toString());
            ShopOrder order = shopOrderRepository.findById(orderId).orElse(null);
            
            if (order == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy đơn hàng"));
            }
            
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng không ở trạng thái chờ thanh toán"));
            }

            String paymentUrl = vnpayService.createPaymentUrl(orderId, order.getTotalAmount(), request);
            
            order.setStatus(OrderStatus.PENDING_VNPAY);
            shopOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentUrl", paymentUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/create-momo")
    public ResponseEntity<?> createMoMoPayment(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long orderId = Long.parseLong(body.get("orderId").toString());
            ShopOrder order = shopOrderRepository.findById(orderId).orElse(null);
            
            if (order == null) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy đơn hàng"));
            }
            
            if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
                return ResponseEntity.badRequest().body(Map.of("message", "Đơn hàng không ở trạng thái chờ thanh toán"));
            }

            String requestId = UUID.randomUUID().toString();
            String paymentUrl = moMoService.createPaymentUrl(orderId, order.getTotalAmount(), requestId);
            
            order.setStatus(OrderStatus.PENDING_MOMO);
            shopOrderRepository.save(order);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "paymentUrl", paymentUrl
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<?> vnpayReturn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.put(key, values[0]);
            }
        });

        boolean valid = vnpayService.validateSignature(params);
        String responseCode = params.get("vnp_ResponseCode");
        
        if (valid && "00".equals(responseCode)) {
            String txnRef = params.get("vnp_TxnRef");
            Long orderId = vnpayService.extractOrderId(txnRef);
            
            if (orderId != null) {
                processSuccessfulPayment(orderId, "VNPay", params.get("vnp_TransactionNo"));
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Thanh toán VNPay thành công!",
                    "redirectUrl", "/my-orders/" + orderId
                ));
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Thanh toán VNPay thất bại hoặc không hợp lệ",
            "redirectUrl", "/"
        ));
    }

    @GetMapping("/momo/return")
    public ResponseEntity<?> momoReturn(@RequestParam Map<String, String> params) {
        String resultCode = params.get("resultCode");
        
        if ("0".equals(resultCode)) {
            String orderIdStr = params.get("orderId");
            if (orderIdStr != null) {
                Long orderId = moMoService.extractOrderId(orderIdStr);
                if (orderId != null) {
                    processSuccessfulPayment(orderId, "MoMo", params.get("transId"));
                    return ResponseEntity.ok(Map.of(
                        "success", true,
                        "message", "Thanh toán MoMo thành công!",
                        "redirectUrl", "/my-orders/" + orderId
                    ));
                }
            }
        }
        
        return ResponseEntity.ok(Map.of(
            "success", false,
            "message", "Thanh toán MoMo thất bại hoặc bị hủy",
            "redirectUrl", "/"
        ));
    }

    @PostMapping("/vnpay/ipn")
    public ResponseEntity<?> vnpayIpn(HttpServletRequest request) {
        Map<String, String> params = new HashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values.length > 0) {
                params.put(key, values[0]);
            }
        });

        if (!vnpayService.validateSignature(params)) {
            return ResponseEntity.badRequest().body("INVALID_SIGNATURE");
        }

        String responseCode = params.get("vnp_ResponseCode");
        if ("00".equals(responseCode)) {
            String txnRef = params.get("vnp_TxnRef");
            Long orderId = vnpayService.extractOrderId(txnRef);
            if (orderId != null) {
                processSuccessfulPayment(orderId, "VNPay", params.get("vnp_TransactionNo"));
                return ResponseEntity.ok("OK");
            }
        }

        return ResponseEntity.ok("NOT_FOUND");
    }

    @PostMapping("/momo/ipn")
    @Transactional
    public ResponseEntity<?> momoIpn(@RequestBody Map<String, Object> params) {
        if (!moMoService.validateSignature(params)) {
            return ResponseEntity.badRequest().body(Map.of("error", "INVALID_SIGNATURE"));
        }

        String resultCode = params.get("resultCode") != null ? params.get("resultCode").toString() : "";
        if ("0".equals(resultCode)) {
            String orderIdStr = params.get("orderId") != null ? params.get("orderId").toString() : "";
            Long orderId = moMoService.extractOrderId(orderIdStr);
            if (orderId != null) {
                processSuccessfulPayment(orderId, "MoMo", params.get("transId") != null ? params.get("transId").toString() : "");
                return ResponseEntity.ok(Map.of("error", 0));
            }
        }

        return ResponseEntity.ok(Map.of("error", 0));
    }

    @Transactional
    private void processSuccessfulPayment(Long orderId, String paymentMethod, String transactionNo) {
        Optional<ShopOrder> orderOpt = shopOrderRepository.findById(orderId);
        if (orderOpt.isEmpty()) return;
        
        ShopOrder order = orderOpt.get();
        if (order.getStatus() == OrderStatus.PAID) return;

        orderFlowService.confirmPaymentForGateway(orderId, paymentMethod, transactionNo);
    }
}
