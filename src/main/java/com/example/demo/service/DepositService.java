package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.DepositRequest;
import com.example.demo.entity.Enum.TransactionAction;
import com.example.demo.entity.TransactionLogEntry;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.DepositRequestRepository;
import com.example.demo.repository.TransactionLogEntryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class DepositService {

    @Autowired
    private DepositRequestRepository depositRequestRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionLogEntryRepository transactionLogEntryRepository;

    @Autowired
    private VNPayService vnpayService;

    @Autowired
    private MoMoService moMoService;

    @Autowired
    private ShopCatalogService shopCatalogService;

    @Value("${app.base-url:http://localhost:8080}")
    private String appBaseUrl;

    private static final int PAYMENT_TIMEOUT_MINUTES = 15;

    /**
     * Tạo yêu cầu nạp tiền và trả về URL thanh toán
     */
    @Transactional
    public Map<String, Object> createDeposit(Long userId, BigDecimal amount, String paymentMethod, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        // Tạo mã đơn nạp tiền duy nhất
        String orderId = generateOrderId(userId);

        // Tạo deposit request
        DepositRequest deposit = new DepositRequest();
        deposit.setUserId(userId);
        deposit.setAmount(amount);
        deposit.setPaymentMethod(paymentMethod);
        deposit.setOrderId(orderId);
        deposit.setStatus(DepositRequest.DepositStatus.PENDING);
        deposit.setExpiresAt(LocalDateTime.now().plusMinutes(PAYMENT_TIMEOUT_MINUTES));

        // Tùy theo phương thức thanh toán
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            return createVNPayPayment(deposit, request);
        } else if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            return createMoMoPayment(deposit, request);
        } else {
            // Mặc định: VietQR ngân hàng (BIDV)
            return createBankQRPayment(deposit);
        }
    }

    /**
     * Tạo QR thanh toán VietQR cho ngân hàng
     */
    private Map<String, Object> createBankQRPayment(DepositRequest deposit) {
        Map<String, Object> result = new HashMap<>();

        // Thông tin ngân hàng BIDV - Shop Acc
        String bankCode = "BIDV";
        String accountNo = "5730278016";
        String accountName = "SHOP ACC";

        // Tạo nội dung chuyển khoản với mã giao dịch
        String transferContent = "NAP" + deposit.getOrderId();

        // Tạo URL QR VietQR
        String qrUrl = String.format(
            "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%.0f&addInfo=%s&accountName=%s",
            bankCode,
            accountNo,
            deposit.getAmount().doubleValue(),
            URLEncoder.encode(transferContent, StandardCharsets.UTF_8),
            URLEncoder.encode(accountName, StandardCharsets.UTF_8)
        );

        deposit.setPaymentMethod("BANK");
        deposit.setQrData(qrUrl);
        depositRequestRepository.save(deposit);

        result.put("success", true);
        result.put("qrUrl", qrUrl);
        result.put("qrCode", null); // Frontend sẽ dùng qrUrl
        result.put("orderId", deposit.getOrderId());
        result.put("amount", deposit.getAmount());
        result.put("amountFormatted", shopCatalogService.formatPrice(deposit.getAmount().doubleValue()));
        result.put("paymentMethod", "BANK");
        result.put("bankCode", bankCode);
        result.put("accountNo", accountNo);
        result.put("accountName", accountName);
        result.put("transferContent", transferContent);
        result.put("expiresAt", deposit.getExpiresAt());

        return result;
    }

    private Map<String, Object> createVNPayPayment(DepositRequest deposit, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tạo URL thanh toán VNPay
            String paymentUrl = vnpayService.createDepositUrl(deposit.getOrderId(), deposit.getAmount().longValue(), request);

            deposit.setPaymentUrl(paymentUrl);
            depositRequestRepository.save(deposit);

            result.put("success", true);
            result.put("paymentUrl", paymentUrl);
            result.put("orderId", deposit.getOrderId());
            result.put("amount", deposit.getAmount());
            result.put("paymentMethod", "VNPAY");
            result.put("expiresAt", deposit.getExpiresAt());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Không thể tạo thanh toán VNPay: " + e.getMessage());
        }

        return result;
    }

    private Map<String, Object> createMoMoPayment(DepositRequest deposit, HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();

        try {
            // Tạo URL thanh toán MoMo
            String requestId = UUID.randomUUID().toString();
            String paymentUrl = moMoService.createDepositUrl(deposit.getOrderId(), deposit.getAmount().longValue(), requestId, request);

            deposit.setPaymentUrl(paymentUrl);
            depositRequestRepository.save(deposit);

            result.put("success", true);
            result.put("paymentUrl", paymentUrl);
            result.put("orderId", deposit.getOrderId());
            result.put("amount", deposit.getAmount());
            result.put("paymentMethod", "MOMO");
            result.put("expiresAt", deposit.getExpiresAt());

        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "Không thể tạo thanh toán MoMo: " + e.getMessage());
        }

        return result;
    }

    /**
     * Xử lý khi thanh toán thành công - TỰ ĐỘNG CỘNG TIỀN
     */
    @Transactional
    public boolean processSuccessfulPayment(String orderId, String transactionId, String paymentMethod) {
        Optional<DepositRequest> optDeposit = depositRequestRepository.findByOrderId(orderId);

        if (optDeposit.isEmpty()) {
            return false;
        }

        DepositRequest deposit = optDeposit.get();

        // Kiểm tra trạng thái
        if (deposit.getStatus() == DepositRequest.DepositStatus.COMPLETED) {
            return true; // Đã xử lý rồi
        }

        if (deposit.getStatus() == DepositRequest.DepositStatus.EXPIRED) {
            return false;
        }

        // Kiểm tra số tiền (VNPay nhân 100)
        BigDecimal paidAmount = deposit.getAmount();
        if ("VNPAY".equalsIgnoreCase(paymentMethod)) {
            // VNPay gửi amount đã nhân 100, cần chia lại
            // Nhưng trong createDepositUrl chúng ta đã xử lý rồi
        }

        // Cập nhật trạng thái deposit
        deposit.setStatus(DepositRequest.DepositStatus.COMPLETED);
        deposit.setTransactionId(transactionId);
        deposit.setCompletedAt(LocalDateTime.now());
        depositRequestRepository.save(deposit);

        // CỘNG TIỀN VÀO VÍ USER
        Account account = accountRepository.findAccountById(deposit.getUserId());
        if (account != null) {
            account.setWallet(account.getWallet() + deposit.getAmount().doubleValue());
            accountRepository.save(account);

            // Tạo log giao dịch
            TransactionLogEntry log = new TransactionLogEntry();
            log.setActorAccountId(deposit.getUserId());
            log.setAmount(deposit.getAmount().doubleValue());
            log.setType("DEPOSIT");
            log.setAction(TransactionAction.DEPOSIT);
            log.setDetail("Nạp tiền qua " + paymentMethod + " - Mã GD: " + transactionId);
            transactionLogEntryRepository.save(log);

            return true;
        }

        return false;
    }

    /**
     * Xử lý khi thanh toán thất bại
     */
    @Transactional
    public void processFailedPayment(String orderId) {
        depositRequestRepository.findByOrderId(orderId).ifPresent(deposit -> {
            deposit.setStatus(DepositRequest.DepositStatus.FAILED);
            depositRequestRepository.save(deposit);
        });
    }

    /**
     * Xử lý khi thanh toán hết hạn
     */
    @Transactional
    public void processExpiredPayment(String orderId) {
        depositRequestRepository.findByOrderId(orderId).ifPresent(deposit -> {
            if (deposit.getStatus() == DepositRequest.DepositStatus.PENDING) {
                deposit.setStatus(DepositRequest.DepositStatus.EXPIRED);
                depositRequestRepository.save(deposit);
            }
        });
    }

    /**
     * Lấy danh sách yêu cầu nạp tiền của user
     */
    public List<DepositRequest> getUserDeposits(Long userId) {
        return depositRequestRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Lấy chi tiết yêu cầu nạp tiền
     */
    public Optional<DepositRequest> getDepositByOrderId(String orderId) {
        return depositRequestRepository.findByOrderId(orderId);
    }

    /**
     * Kiểm tra và xử lý các yêu cầu hết hạn
     */
    @Transactional
    public void expireOldDeposits() {
        List<DepositRequest> pendingDeposits = depositRequestRepository.findByStatus(DepositRequest.DepositStatus.PENDING);
        LocalDateTime now = LocalDateTime.now();

        for (DepositRequest deposit : pendingDeposits) {
            if (deposit.getExpiresAt() != null && deposit.getExpiresAt().isBefore(now)) {
                deposit.setStatus(DepositRequest.DepositStatus.EXPIRED);
                depositRequestRepository.save(deposit);
            }
        }
    }

    /**
     * Tạo mã đơn nạp tiền duy nhất
     */
    private String generateOrderId(Long userId) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = String.format("%04d", new Random().nextInt(10000));
        String orderId = "NAP" + timestamp + random;

        // Đảm bảo không trùng lặp
        while (depositRequestRepository.existsByOrderId(orderId)) {
            orderId = "NAP" + System.currentTimeMillis() + String.format("%04d", new Random().nextInt(10000));
        }

        return orderId;
    }

    /**
     * Format số tiền thành chuỗi có định dạng
     */
    public String formatAmount(double amount) {
        return shopCatalogService.formatPrice(amount);
    }

    /**
     * Xác nhận thanh toán thủ công (khi dùng VietQR)
     * User bấm "Đã chuyển khoản" sau khi quét QR
     */
    @Transactional
    public boolean confirmManualPayment(String orderId, Long userId) {
        Optional<DepositRequest> optDeposit = depositRequestRepository.findByOrderId(orderId);

        if (optDeposit.isEmpty()) {
            return false;
        }

        DepositRequest deposit = optDeposit.get();

        // Kiểm tra quyền sở hữu
        if (!deposit.getUserId().equals(userId)) {
            return false;
        }

        // Kiểm tra trạng thái
        if (deposit.getStatus() != DepositRequest.DepositStatus.PENDING) {
            return false;
        }

        // Kiểm tra hết hạn
        if (deposit.getExpiresAt() != null && deposit.getExpiresAt().isBefore(LocalDateTime.now())) {
            deposit.setStatus(DepositRequest.DepositStatus.EXPIRED);
            depositRequestRepository.save(deposit);
            return false;
        }

        // Cập nhật trạng thái thành completed
        deposit.setStatus(DepositRequest.DepositStatus.COMPLETED);
        deposit.setCompletedAt(LocalDateTime.now());
        depositRequestRepository.save(deposit);

        // CỘNG TIỀN VÀO VÍ USER
        Account account = accountRepository.findAccountById(deposit.getUserId());
        if (account != null) {
            account.setWallet(account.getWallet() + deposit.getAmount().doubleValue());
            accountRepository.save(account);

            // Tạo log giao dịch
            TransactionLogEntry log = new TransactionLogEntry();
            log.setActorAccountId(deposit.getUserId());
            log.setAmount(deposit.getAmount().doubleValue());
            log.setType("DEPOSIT");
            log.setAction(TransactionAction.DEPOSIT);
            log.setDetail("Nạp tiền qua VietQR - Mã GD: " + orderId);
            transactionLogEntryRepository.save(log);

            return true;
        }

        return false;
    }

    /**
     * Validate VNPay signature cho deposit
     */
    public boolean validateVNPaySignature(Map<String, String> params) {
        return vnpayService.validateDepositSignature(params);
    }

    /**
     * Validate MoMo signature cho deposit
     */
    public boolean validateMoMoSignature(Map<String, Object> params) {
        return moMoService.validateDepositSignature(params);
    }
}
