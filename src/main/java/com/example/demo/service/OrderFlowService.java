package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.Enum.TransactionAction;
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

    @Transactional
    public ShopOrder createOrderFromSlugs(Long buyerId, Collection<String> productSlugs) {
        if (productSlugs == null || productSlugs.isEmpty()) {
            throw new IllegalArgumentException("Không có sản phẩm trong đơn.");
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>(productSlugs);
        ShopOrder order = new ShopOrder();
        order.setBuyerId(buyerId);
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
        log(saved.getId(), buyerId, TransactionAction.ORDER_CREATED, "Tạo đơn #" + saved.getId() + ", tổng " + total);
        return saved;
    }

    @Transactional
    public void confirmPayment(Long orderId, Long adminId) {
        ShopOrder order = shopOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn."));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Đơn không ở trạng thái chờ thanh toán.");
        }
        Date now = new Date();
        for (OrderLine line : order.getLines()) {
            InventoryItem pick = inventoryItemRepository
                    .pickRandomAvailable(line.getProduct().getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Hết acc kho cho: " + line.getProduct().getName() + " — nhập thêm acc trước khi xác nhận."));
            pick.setStatus(InventoryItemStatus.SOLD);
            inventoryItemRepository.save(pick);
            line.setAssignedInventory(pick);
            line.setDeliveredCredentials(pick.getCredentials());
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
