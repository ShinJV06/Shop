package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.Enum.TransactionAction;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ShopOrderRepository;
import com.example.demo.repository.TransactionLogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class OrderFlowService {

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private TransactionLogEntryRepository transactionLogEntryRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Transactional
    public ShopOrder createOrderFromSlugs(Long buyerId, Collection<String> productSlugs, String paymentMethod) {
        if (productSlugs == null || productSlugs.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm trong đơn.");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>(productSlugs);
        ShopOrder order = new ShopOrder();
        order.setBuyerId(buyerId);
        order.setPaymentMethod(paymentMethod);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        long total = 0;

        for (String slug : unique) {
            Product product = productRepository.findBySlug(slug)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại: " + slug));
            if (!product.isVisible()) {
                throw new IllegalArgumentException("Sản phẩm đã ẩn: " + slug);
            }
            OrderLine line = new OrderLine();
            line.setProduct(product);
            line.setProductNameSnapshot(product.getName());
            line.setUnitPrice(product.getPrice());
            line.attachOrder(order);
            total += product.getPrice();
        }
        order.setTotalAmount(total);
        ShopOrder saved = shopOrderRepository.save(order);
        log(saved.getId(), buyerId, TransactionAction.ORDER_CREATED, "Tạo đơn #" + saved.getId() + ", tổng " + total + ", TT: " + paymentMethod);

        // Thanh toán ví → trừ tiền, giao acc luôn
        if ("WALLET".equals(paymentMethod)) {
            Account buyer = accountRepository.findAccountById(buyerId);
            if (buyer == null) {
                throw new IllegalStateException("Không tìm thấy tài khoản.");
            }
            if (buyer.getWallet() < total) {
                throw new IllegalStateException("Số dư ví không đủ! Số dư: " + (long)buyer.getWallet() + "đ, cần: " + total + "đ");
            }
            buyer.setWallet(buyer.getWallet() - total);
            accountRepository.save(buyer);
            TransactionLogEntry walletLog = new TransactionLogEntry();
            walletLog.setOrderId(saved.getId());
            walletLog.setActorAccountId(buyerId);
            walletLog.setAction(TransactionAction.PAYMENT_CONFIRMED);
            walletLog.setType("PAYMENT");
            walletLog.setAmount((double) -total);
            walletLog.setDetail("Thanh toán đơn #" + saved.getId() + " bằng ví, trừ " + total + "đ");
            transactionLogEntryRepository.save(walletLog);

            Date now = new Date();
            for (OrderLine line : saved.getLines()) {
                // TEST: tạm bỏ kiểm tra tồn kho để test thanh toán
                Optional<InventoryItem> pickOpt = inventoryItemRepository
                        .pickRandomAvailable(line.getProduct().getId());
                if (pickOpt.isPresent()) {
                    InventoryItem pick = pickOpt.get();
                    pick.setStatus(InventoryItemStatus.SOLD);
                    inventoryItemRepository.save(pick);
                    line.setAssignedInventory(pick);
                    line.setDeliveredCredentials(pick.getCredentials());
                } else {
                    line.setDeliveredCredentials("ACC_TEST_" + line.getProductNameSnapshot() + "_" + saved.getId());
                }
            }
            saved.setStatus(OrderStatus.PAID);
            saved.setPaidAt(now);
            shopOrderRepository.save(saved);
            log(saved.getId(), buyerId, TransactionAction.PAYMENT_CONFIRMED, "Thanh toán ví tự động, đã gán acc.");
        }
        return saved;
    }

    @Transactional
    public void confirmPayment(Long orderId, Long adminId) {
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Đơn không ở trạng thái chờ thanh toán.");
        }
        Account buyer = accountRepository.findAccountById(order.getBuyerId());
        if (buyer == null) {
            throw new IllegalStateException("Không tìm thấy tài khoản người mua.");
        }

        // WALLET: trừ tiền từ ví người mua ngay
        if ("WALLET".equals(order.getPaymentMethod())) {
            if (buyer.getWallet() < order.getTotalAmount()) {
                throw new IllegalStateException("Số dư ví không đủ! Số dư hiện tại: " + (long)buyer.getWallet() + "đ, cần: " + order.getTotalAmount() + "đ");
            }
            buyer.setWallet(buyer.getWallet() - order.getTotalAmount());
            accountRepository.save(buyer);
            TransactionLogEntry walletLog = new TransactionLogEntry();
            walletLog.setOrderId(orderId);
            walletLog.setActorAccountId(order.getBuyerId());
            walletLog.setAction(TransactionAction.PAYMENT_CONFIRMED);
            walletLog.setType("PAYMENT");
            walletLog.setAmount((double) -order.getTotalAmount());
            walletLog.setDetail("Thanh toán đơn #" + orderId + " bằng ví, trừ " + order.getTotalAmount() + "đ");
            transactionLogEntryRepository.save(walletLog);
        }
        // BANK: đơn đã chuyển khoản, admin xác nhận là đã nhận tiền

        Date now = new Date();
        for (OrderLine line : order.getLines()) {
            // TEST: tạm bỏ kiểm tra tồn kho để test thanh toán
            Optional<InventoryItem> pickOpt = inventoryItemRepository
                    .pickRandomAvailable(line.getProduct().getId());
            if (pickOpt.isPresent()) {
                InventoryItem pick = pickOpt.get();
                pick.setStatus(InventoryItemStatus.SOLD);
                inventoryItemRepository.save(pick);
                line.setAssignedInventory(pick);
                line.setDeliveredCredentials(pick.getCredentials());
            } else {
                line.setDeliveredCredentials("ACC_TEST_" + line.getProductNameSnapshot() + "_" + order.getId());
            }
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaidAt(now);
        shopOrderRepository.save(order);
        log(orderId, adminId, TransactionAction.PAYMENT_CONFIRMED, "Admin xác nhận thanh toán, đã gán acc ngẫu nhiên.");
    }

    @Transactional
    public void refundOrder(Long orderId, Long adminId) {
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        if (order.getStatus() == OrderStatus.REFUNDED) {
            return;
        }
        if (order.getStatus() == OrderStatus.PAID) {
            // Hoàn tiền vào ví
            Account buyer = accountRepository.findAccountById(order.getBuyerId());
            if (buyer != null) {
                buyer.setWallet(buyer.getWallet() + order.getTotalAmount());
                accountRepository.save(buyer);

                // Log hoàn tiền
                TransactionLogEntry refundLog = new TransactionLogEntry();
                refundLog.setOrderId(orderId);
                refundLog.setActorAccountId(order.getBuyerId());
                refundLog.setAction(TransactionAction.REFUNDED);
                refundLog.setType("REFUND");
                refundLog.setAmount((double) order.getTotalAmount());
                refundLog.setDetail("Hoàn tiền đơn #" + orderId + ", +" + order.getTotalAmount() + "đ vào ví");
                transactionLogEntryRepository.save(refundLog);
            }

            for (OrderLine line : order.getLines()) {
                if (line.getAssignedInventory() != null) {
                    InventoryItem inv = line.getAssignedInventory();
                    inv.setStatus(InventoryItemStatus.AVAILABLE);
                    inventoryItemRepository.save(inv);
                }
                line.setAssignedInventory(null);
                line.setDeliveredCredentials(null);
            }
        }
        order.setStatus(OrderStatus.REFUNDED);
        order.setPaidAt(null);
        shopOrderRepository.save(order);
        log(orderId, adminId, TransactionAction.REFUNDED, "Hoàn tiền / hủy giao acc.");
    }

    public List<TransactionLogEntry> logsForOrder(Long orderId) {
        return transactionLogEntryRepository.findAll().stream()
            .filter(t -> orderId.equals(t.getOrderId()))
            .sorted(Comparator.comparing(TransactionLogEntry::getId).reversed())
            .collect(java.util.stream.Collectors.toList());
    }

    private void log(Long orderId, Long actor, TransactionAction action, String detail) {
        TransactionLogEntry e = new TransactionLogEntry();
        e.setOrderId(orderId);
        e.setActorAccountId(actor);
        e.setAction(action);
        e.setDetail(detail);
        transactionLogEntryRepository.save(e);
    }
}
