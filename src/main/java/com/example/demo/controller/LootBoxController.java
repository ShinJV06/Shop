package com.example.demo.controller;

import com.example.demo.entity.LootBox;
import com.example.demo.entity.LootBoxHistory;
import com.example.demo.entity.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.service.LootBoxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lootbox")
public class LootBoxController {

    @Autowired
    private LootBoxService lootBoxService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/list")
    public ResponseEntity<?> getLootBoxes() {
        List<LootBox> lootBoxes = lootBoxService.getActiveLootBoxes();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", lootBoxes
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getRecentHistory(@RequestParam(defaultValue = "20") int limit) {
        List<LootBoxHistory> history = lootBoxService.getRecentHistory(limit);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", history
        ));
    }

    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory(@RequestParam Long userId) {
        List<LootBoxHistory> history = lootBoxService.getUserHistory(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", history
        ));
    }

    @PostMapping("/open")
    public ResponseEntity<?> openLootBox(@RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(body.get("userId").toString());
        Long lootBoxId = Long.parseLong(body.get("lootBoxId").toString());

        // Lấy thông tin user
        Account user = accountRepository.findAccountById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Không tìm thấy người dùng"
            ));
        }

        // Mở rương
        LootBoxService.LootBoxResult result = lootBoxService.openLootBox(userId, lootBoxId);

        if (!result.success) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", result.error,
                "balance", user.getWallet()
            ));
        }

        // Trả về kết quả
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isWin", result.isWin);
        response.put("newBalance", result.newBalance);
        response.put("credentials", result.credentials);

        if (result.lootBox != null) {
            response.put("lootBoxName", result.lootBox.getName());
            response.put("lootBoxTier", result.lootBox.getTier());
            response.put("price", result.lootBox.getPrice());
        }

        if (result.reward != null) {
            response.put("rewardName", result.reward.getName());
            response.put("rewardRarity", result.reward.getRarity());
            response.put("rewardDescription", result.reward.getDescription());
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance/{userId}")
    public ResponseEntity<?> getBalance(@PathVariable Long userId) {
        Account user = accountRepository.findAccountById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Không tìm thấy người dùng"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "success", true,
            "balance", user.getWallet()
        ));
    }

    @PostMapping("/init")
    public ResponseEntity<?> initializeLootBoxes() {
        lootBoxService.initializeDefaultLootBoxes();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Đã khởi tạo loot boxes mặc định"
        ));
    }
}
