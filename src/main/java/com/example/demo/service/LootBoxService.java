package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.TransactionAction;
import com.example.demo.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class LootBoxService {

    @Autowired
    private LootBoxRepository lootBoxRepository;

    @Autowired
    private LootBoxHistoryRepository historyRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NotificationService notificationService;

    public List<LootBox> getActiveLootBoxes() {
        return lootBoxRepository.findAllActiveWithRewards();
    }

    public List<LootBoxHistory> getRecentHistory(int limit) {
        return historyRepository.findRecentHistory(org.springframework.data.domain.PageRequest.of(0, limit));
    }

    public List<LootBoxHistory> getUserHistory(Long userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public LootBoxResult openLootBox(Long userId, Long lootBoxId) {
        Account user = accountRepository.findAccountById(userId);
        if (user == null) {
            return LootBoxResult.error("Không tìm thấy người dùng");
        }

        LootBox lootBox = lootBoxRepository.findByIdWithRewards(lootBoxId)
                .orElse(null);
        if (lootBox == null || !lootBox.isActive()) {
            return LootBoxResult.error("Không tìm thấy rương");
        }

        // Kiểm tra số dư
        if (user.getWallet() < lootBox.getPrice()) {
            return LootBoxResult.error("Số dư không đủ! Cần " + lootBox.getPrice() + "đ");
        }

        // Trừ tiền
        user.setWallet(user.getWallet() - lootBox.getPrice());
        accountRepository.save(user);

        // Ghi log giao dịch
        TransactionLogEntry logEntry = new TransactionLogEntry();
        logEntry.setActorAccountId(userId);
        logEntry.setAction(TransactionAction.PURCHASE);
        logEntry.setType("LOOTBOX");
        logEntry.setAmount((double) -lootBox.getPrice());
        logEntry.setDetail("Mở " + lootBox.getName() + " - Trừ " + lootBox.getPrice() + "đ");
        // logEntryRepository.save(logEntry);

        // Roll thưởng
        LootBoxReward reward = rollReward(lootBox);
        
        boolean isWin = reward != null && reward.getQuantity() > 0;
        String credentials = null;
        Long orderId = null;

        if (isWin) {
            // Trừ số lượng reward
            reward.setQuantity(reward.getQuantity() - 1);
            
            // Giao tài khoản từ inventory
            if (reward.getProduct() != null) {
                Optional<InventoryItem> itemOpt = inventoryItemRepository
                        .pickRandomAvailable(reward.getProduct().getId());
                
                if (itemOpt.isPresent()) {
                    InventoryItem item = itemOpt.get();
                    item.setStatus(InventoryItemStatus.SOLD);
                    item.setBuyerId(userId);
                    item.setSoldAt(new Date());
                    inventoryItemRepository.save(item);
                    credentials = item.getCredentials();
                } else {
                    // Fallback: tạo tài khoản test
                    credentials = "TEST_" + reward.getProductNameSnapshot() + "_" + System.currentTimeMillis();
                }
            } else {
                // Không có product, tạo tài khoản test
                credentials = "TEST_" + reward.getName() + "_" + System.currentTimeMillis();
            }
        }

        // Lưu lịch sử
        LootBoxHistory history = new LootBoxHistory();
        history.setUserId(userId);
        history.setLootBoxId(lootBoxId);
        history.setLootBoxName(lootBox.getName());
        history.setTier(lootBox.getTier());
        history.setPrice(lootBox.getPrice());
        history.setRewardId(reward != null ? reward.getId() : null);
        history.setRewardName(reward != null ? reward.getName() : "Không trúng thưởng");
        history.setRewardRarity(reward != null ? reward.getRarity() : "NONE");
        history.setWin(isWin);
        history.setCredentials(credentials);
        historyRepository.save(history);

        // Cập nhật số dư mới
        long newBalance = (long) user.getWallet();

        log.info("User {} opened {} - Won: {} ({})", userId, lootBox.getName(), 
                reward != null ? reward.getName() : "Nothing", 
                reward != null ? reward.getRarity() : "N/A");

        return LootBoxResult.success(
                lootBox,
                reward,
                credentials,
                newBalance,
                isWin
        );
    }

    private LootBoxReward rollReward(LootBox lootBox) {
        List<LootBoxReward> availableRewards = new ArrayList<>();
        int totalChance = 0;

        for (LootBoxReward reward : lootBox.getRewards()) {
            if (reward.getQuantity() > 0) {
                availableRewards.add(reward);
                totalChance += reward.getChance();
            }
        }

        if (availableRewards.isEmpty()) {
            return null;
        }

        int roll = new Random().nextInt(totalChance);
        int cumulative = 0;

        for (LootBoxReward reward : availableRewards) {
            cumulative += reward.getChance();
            if (roll < cumulative) {
                return reward;
            }
        }

        return availableRewards.get(0);
    }

    @Transactional
    public void initializeDefaultLootBoxes() {
        if (lootBoxRepository.count() > 0) return;

        // Rương Thường
        LootBox common = new LootBox();
        common.setName("Rương Thường");
        common.setTier("COMMON");
        common.setPrice(10000);
        common.setDescription("Tỷ lệ cao nhận Acc 4★");
        common = lootBoxRepository.save(common);

        // Rương Hiếm
        LootBox rare = new LootBox();
        rare.setName("Rương Hiếm");
        rare.setTier("RARE");
        rare.setPrice(20000);
        rare.setDescription("Cơ hội nhận Acc 5★");
        rare = lootBoxRepository.save(rare);

        // Rương Huyền Thoại
        LootBox legendary = new LootBox();
        legendary.setName("Rương Huyền Thoại");
        legendary.setTier("LEGENDARY");
        legendary.setPrice(50000);
        legendary.setDescription("Tỷ lệ cao nhận Acc Limited");
        legendary = lootBoxRepository.save(legendary);

        log.info("Initialized 3 default loot boxes");
    }

    // Result class
    public static class LootBoxResult {
        public boolean success;
        public String error;
        public LootBox lootBox;
        public LootBoxReward reward;
        public String credentials;
        public long newBalance;
        public boolean isWin;

        public static LootBoxResult success(LootBox box, LootBoxReward reward, 
                String credentials, long newBalance, boolean isWin) {
            LootBoxResult result = new LootBoxResult();
            result.success = true;
            result.lootBox = box;
            result.reward = reward;
            result.credentials = credentials;
            result.newBalance = newBalance;
            result.isWin = isWin;
            return result;
        }

        public static LootBoxResult error(String message) {
            LootBoxResult result = new LootBoxResult();
            result.success = false;
            result.error = message;
            return result;
        }
    }
}
