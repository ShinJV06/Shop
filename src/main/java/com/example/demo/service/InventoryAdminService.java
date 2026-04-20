package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Enum.Role;
import com.example.demo.entity.Enum.TransactionAction;
import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Product;
import com.example.demo.entity.TransactionLogEntry;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.TransactionLogEntryRepository;
import com.example.demo.util.CredentialHasher;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryAdminService {

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionLogEntryRepository transactionLogEntryRepository;

    @Autowired
    private ListingImageStorageService listingImageStorageService;

    @Transactional
    public InventoryItem addSingle(
            Long productId,
            String game,
            String rankInfo,
            String skinInfo,
            String extraInfo,
            String credentials,
            Account actor,
            MultipartFile listingImage,
            BigDecimal customPrice
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
        String hash = CredentialHasher.sha256(credentials);
        if (inventoryItemRepository.existsByCredentialHash(hash)) {
            throw new IllegalStateException("Acc trùng (đã có cùng nội dung credentials trong hệ thống).");
        }
        String imagePath = resolveListingImagePath(actor, product, listingImage);
        InventoryItem item = new InventoryItem();
        item.setProduct(product);
        item.setGame(game);
        item.setRankInfo(rankInfo);
        item.setSkinInfo(skinInfo);
        item.setExtraInfo(extraInfo);
        item.setPrice(customPrice);
        item.setCredentials(credentials.trim());
        item.setCredentialHash(hash);
        item.setListingImagePath(imagePath);
        item.setStatus(InventoryItemStatus.AVAILABLE);
        if (actor.getRole() == Role.CTV) {
            item.setModerationStatus(ModerationStatus.PENDING);
            item.setSubmittedById(actor.getId());
        } else {
            item.setModerationStatus(ModerationStatus.APPROVED);
            item.setSubmittedById(actor.getId());
        }
        InventoryItem saved = inventoryItemRepository.save(item);
        log(null, actor.getId(), TransactionAction.INVENTORY_IMPORTED, "Thêm acc id=" + saved.getId() + " cho SP " + product.getSlug() + (customPrice != null ? " giá " + customPrice : ""));
        return saved;
    }

    private String resolveListingImagePath(Account actor, Product product, MultipartFile listingImage) {
        if (actor.getRole() == Role.CTV) {
            if (listingImage == null || listingImage.isEmpty()) {
                throw new IllegalArgumentException("CTV phải đính kèm ảnh minh hoạ acc.");
            }
            return listingImageStorageService.store(listingImage);
        }
        if (listingImage != null && !listingImage.isEmpty()) {
            return listingImageStorageService.store(listingImage);
        }
        return product.getImagePath();
    }

    @Transactional
    public int importBulk(Long productId, InputStream inputStream, String defaultGame, String defaultRank, Account actor) throws Exception {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));
        int added = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String game = defaultGame != null ? defaultGame : "";
                String rank = defaultRank != null ? defaultRank : "";
                String skin = "";
                String extra = "";
                String cred = trimmed;

                if (trimmed.contains("|")) {
                    String[] parts = trimmed.split("\\|", -1);
                    cred = parts[0].trim();
                    if (parts.length > 1) {
                        game = parts[1].trim();
                    }
                    if (parts.length > 2) {
                        rank = parts[2].trim();
                    }
                    if (parts.length > 3) {
                        skin = parts[3].trim();
                    }
                    if (parts.length > 4) {
                        extra = parts[4].trim();
                    }
                } else if (trimmed.contains(",")) {
                    String[] parts = trimmed.split(",", -1);
                    if (parts.length >= 2 && parts[0].trim().length() < 120) {
                        cred = parts[parts.length - 1].trim();
                        game = parts.length > 0 ? parts[0].trim() : game;
                        rank = parts.length > 1 ? parts[1].trim() : rank;
                        skin = parts.length > 2 ? parts[2].trim() : skin;
                        extra = parts.length > 3 ? parts[3].trim() : extra;
                    }
                }

                if (cred.isEmpty()) {
                    errors.add("Dòng " + lineNo + ": thiếu credentials.");
                    continue;
                }
                String hash = CredentialHasher.sha256(cred);
                if (inventoryItemRepository.existsByCredentialHash(hash)) {
                    errors.add("Dòng " + lineNo + ": trùng acc.");
                    continue;
                }
                InventoryItem item = new InventoryItem();
                item.setProduct(product);
                item.setGame(game);
                item.setRankInfo(rank);
                item.setSkinInfo(skin);
                item.setExtraInfo(extra);
                item.setCredentials(cred);
                item.setCredentialHash(hash);
                item.setListingImagePath(product.getImagePath());
                item.setStatus(InventoryItemStatus.AVAILABLE);
                if (actor.getRole() == Role.CTV) {
                    item.setModerationStatus(ModerationStatus.PENDING);
                } else {
                    item.setModerationStatus(ModerationStatus.APPROVED);
                }
                item.setSubmittedById(actor.getId());
                inventoryItemRepository.save(item);
                added++;
            }
        }
        log(null, actor.getId(), TransactionAction.INVENTORY_IMPORTED, "Import file: " + added + " acc cho " + product.getSlug() + ". Lỗi: " + errors.size());
        if (!errors.isEmpty() && added == 0) {
            throw new IllegalStateException(String.join("; ", errors.subList(0, Math.min(5, errors.size()))));
        }
        return added;
    }

    @Transactional
    public void setHidden(Long itemId, boolean hidden) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy acc."));
        item.setHidden(hidden);
        inventoryItemRepository.save(item);
    }

    @Transactional
    public void setStatus(Long itemId, InventoryItemStatus status) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy acc."));
        item.setStatus(status);
        inventoryItemRepository.save(item);
    }

    @Transactional
    public void approve(Long itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy acc."));
        item.setModerationStatus(ModerationStatus.APPROVED);
        inventoryItemRepository.save(item);
    }

    @Transactional
    public void reject(Long itemId) {
        InventoryItem item = inventoryItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy acc."));
        item.setModerationStatus(ModerationStatus.REJECTED);
        item.setStatus(InventoryItemStatus.ERROR);
        inventoryItemRepository.save(item);
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
