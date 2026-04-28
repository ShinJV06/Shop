package com.example.demo.service;

import com.example.demo.entity.*;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.repository.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
public class MysteryBagService {

    @Autowired
    private MysteryBagRepository mysteryBagRepository;

    @Autowired
    private MysteryBagRewardRepository rewardRepository;

    @Autowired
    private MysteryBagHistoryRepository historyRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    public List<MysteryBag> getActiveMysteryBags() {
        return mysteryBagRepository.findAllActive();
    }

    public List<MysteryBagHistory> getRecentHistory(int limit) {
        return historyRepository.findAllByOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
    }

    public List<MysteryBagHistory> getUserHistory(Long userId) {
        return historyRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public MysteryBagResult openMysteryBag(Long userId, Long mysteryBagId) {
        Account user = accountRepository.findAccountById(userId);
        if (user == null) {
            return MysteryBagResult.error("Không tìm thấy người dùng");
        }

        MysteryBag mysteryBag = mysteryBagRepository.findById(mysteryBagId).orElse(null);
        if (mysteryBag == null || !mysteryBag.isActive()) {
            return MysteryBagResult.error("Không tìm thấy túi mù");
        }

        int price = mysteryBag.getPrice();
        double currentBalance = user.getWallet();
        
        if (currentBalance < price) {
            return MysteryBagResult.error("Số dư không đủ! Cần " + price + "đ");
        }

        // Tính số dư mới TRƯỚC KHI trừ
        double newBalance = currentBalance - price;

        // Trừ tiền
        user.setWallet(newBalance);
        accountRepository.save(user);

        MysteryBagReward reward = rollReward(mysteryBagId);
        
        // Tất cả các reward đều là "trúng" - quantity chỉ là số lượng có sẵn
        boolean isWin = reward != null;
        String credentials = null;

        if (reward != null) {
            // Ưu tiên lấy item từ inventory nếu có product
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
                    // Không có item trong kho, tạo credentials từ reward
                    credentials = "ACC_" + reward.getName() + "_" + System.currentTimeMillis();
                }
            } else {
                // Không có product cụ thể, tạo credentials từ tên reward
                credentials = "ACC_" + reward.getName() + "_" + System.currentTimeMillis();
            }
        }

        MysteryBagHistory history = new MysteryBagHistory();
        history.setUserId(userId);
        history.setMysteryBagId(mysteryBagId);
        history.setMysteryBagName(mysteryBag.getName());
        history.setRewardId(reward != null ? reward.getId() : null);
        history.setRewardName(reward != null ? reward.getName() : "Không trúng thưởng");
        history.setRewardRarity(reward != null ? reward.getRarity() : "NONE");
        history.setWin(isWin);
        history.setCredentials(credentials);
        history.setPrice(price);
        historyRepository.save(history);

        log.info("User {} opened MysteryBag {} - Won: {} ({})", userId, mysteryBag.getName(), 
                reward != null ? reward.getName() : "Nothing", 
                reward != null ? reward.getRarity() : "N/A");

        return MysteryBagResult.success(
                mysteryBag,
                reward,
                credentials,
                (long) newBalance,
                isWin
        );
    }

    private MysteryBagReward rollReward(Long mysteryBagId) {
        List<MysteryBagReward> rewards = rewardRepository.findByMysteryBagId(mysteryBagId);
        
        if (rewards == null || rewards.isEmpty()) {
            log.warn("No rewards found for mystery bag {}", mysteryBagId);
            return null;
        }
        
        // Filter out rewards with 0 chance
        List<MysteryBagReward> validRewards = rewards.stream()
                .filter(r -> r.getChance() > 0)
                .toList();
        
        if (validRewards.isEmpty()) {
            log.warn("No valid rewards (chance > 0) for mystery bag {}", mysteryBagId);
            return null;
        }
        
        int totalChance = validRewards.stream().mapToInt(MysteryBagReward::getChance).sum();

        if (totalChance == 0) {
            return null;
        }

        int roll = new Random().nextInt(totalChance);
        int cumulative = 0;

        for (MysteryBagReward reward : validRewards) {
            cumulative += reward.getChance();
            if (roll < cumulative) {
                log.debug("Roll {} matched reward {} with chance {} (cumulative: {})", roll, reward.getName(), reward.getChance(), cumulative);
                return reward;
            }
        }

        return validRewards.get(0);
    }

    @Transactional
    public void initializeDefaultMysteryBags() {
        if (mysteryBagRepository.count() > 0) return;

        MysteryBag basic = new MysteryBag();
        basic.setName("Túi Cơ Bản");
        basic.setPrice(5000);
        basic.setDescription("Tỷ lệ cao nhận Acc 4★");
        basic = mysteryBagRepository.save(basic);

        createReward(basic, "Acc 3★ Ngẫu Nhiên", "COMMON", 60, 10);
        createReward(basic, "Acc 4★ Hiếm", "RARE", 30, 5);
        createReward(basic, "Acc 5★ Huyền Thoại", "LEGENDARY", 10, 1);

        MysteryBag special = new MysteryBag();
        special.setName("Túi Đặc Biệt");
        special.setPrice(10000);
        special.setDescription("Cơ hội nhận Acc 5★");
        special = mysteryBagRepository.save(special);

        createReward(special, "Acc 3★ Ngẫu Nhiên", "COMMON", 40, 10);
        createReward(special, "Acc 4★ Hiếm", "RARE", 40, 5);
        createReward(special, "Acc 5★ Huyền Thoại", "LEGENDARY", 20, 2);

        MysteryBag legendary = new MysteryBag();
        legendary.setName("Túi Huyền Thoại");
        legendary.setPrice(20000);
        legendary.setDescription("Tỷ lệ cao nhận Acc Limited");
        legendary = mysteryBagRepository.save(legendary);

        createReward(legendary, "Acc 4★ Hiếm", "RARE", 50, 5);
        createReward(legendary, "Acc 5★ Huyền Thoại", "LEGENDARY", 50, 3);

        log.info("Initialized 3 mystery bags with rewards");
    }

    private void createReward(MysteryBag bag, String name, String rarity, int chance, int quantity) {
        MysteryBagReward reward = new MysteryBagReward();
        reward.setMysteryBag(bag);
        reward.setName(name);
        reward.setRarity(rarity);
        reward.setChance(chance);
        reward.setQuantity(quantity);
        reward.setDescription(name);
        rewardRepository.save(reward);
    }

    public static class MysteryBagResult {
        public boolean success;
        public String error;
        public MysteryBag mysteryBag;
        public MysteryBagReward reward;
        public String credentials;
        public long newBalance;
        public boolean isWin;

        public static MysteryBagResult success(MysteryBag bag, MysteryBagReward reward, 
                String credentials, long newBalance, boolean isWin) {
            MysteryBagResult result = new MysteryBagResult();
            result.success = true;
            result.mysteryBag = bag;
            result.reward = reward;
            result.credentials = credentials;
            result.newBalance = newBalance;
            result.isWin = isWin;
            return result;
        }

        public static MysteryBagResult error(String message) {
            MysteryBagResult result = new MysteryBagResult();
            result.success = false;
            result.error = message;
            return result;
        }
    }
}
