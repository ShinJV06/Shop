package com.example.demo.controller;

import com.example.demo.entity.MysteryBag;
import com.example.demo.entity.MysteryBagHistory;
import com.example.demo.entity.Account;
import com.example.demo.repository.AccountRepository;
import com.example.demo.service.MysteryBagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mysterybag")
public class MysteryBagController {

    @Autowired
    private MysteryBagService mysteryBagService;

    @Autowired
    private AccountRepository accountRepository;

    @GetMapping("/list")
    public ResponseEntity<?> getMysteryBags() {
        List<MysteryBag> bags = mysteryBagService.getActiveMysteryBags();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", bags
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getRecentHistory(@RequestParam(defaultValue = "20") int limit) {
        List<MysteryBagHistory> history = mysteryBagService.getRecentHistory(limit);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", history
        ));
    }

    @GetMapping("/my-history")
    public ResponseEntity<?> getMyHistory(@RequestParam Long userId) {
        List<MysteryBagHistory> history = mysteryBagService.getUserHistory(userId);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "data", history
        ));
    }

    @PostMapping("/open")
    public ResponseEntity<?> openMysteryBag(@RequestBody Map<String, Object> body) {
        Long userId = Long.parseLong(body.get("userId").toString());
        Long mysteryBagId = Long.parseLong(body.get("mysteryBagId").toString());

        Account user = accountRepository.findAccountById(userId);
        if (user == null) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "Không tìm thấy người dùng"
            ));
        }

        MysteryBagService.MysteryBagResult result = mysteryBagService.openMysteryBag(userId, mysteryBagId);

        if (!result.success) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", result.error,
                "balance", user.getWallet()
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isWin", result.isWin);
        response.put("newBalance", result.newBalance);
        response.put("credentials", result.credentials);

        if (result.mysteryBag != null) {
            response.put("mysteryBagName", result.mysteryBag.getName());
            response.put("price", result.mysteryBag.getPrice());
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
    public ResponseEntity<?> initializeMysteryBags() {
        mysteryBagService.initializeDefaultMysteryBags();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Đã khởi tạo túi mù mặc định"
        ));
    }

    @GetMapping("/test")
    public ResponseEntity<?> testMysteryBag() {
        List<MysteryBag> bags = mysteryBagService.getActiveMysteryBags();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "bagsCount", bags.size(),
            "bags", bags
        ));
    }
}
