package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.DailyLogin;
import com.example.demo.repository.AccountRepository;
import com.example.demo.service.DailyLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/daily-login")
public class DailyLoginController {

    @Autowired
    private DailyLoginService dailyLoginService;

    @Autowired
    private AccountRepository accountRepository;

    private String getUsernameFromAuth(Authentication auth) {
        if (auth == null) return null;

        if (auth instanceof OAuth2AuthenticationToken oauthToken) {
            Map<String, Object> attributes = oauthToken.getPrincipal().getAttributes();
            String email = (String) attributes.get("email");
            if (email != null) {
                Account account = accountRepository.findAccountByEmail(email);
                if (account != null) {
                    return account.getUsername();
                }
            }
        }

        return auth.getName();
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDailyLoginStatus(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        String username = getUsernameFromAuth(auth);
        Map<String, Object> status = dailyLoginService.checkDailyLogin(username);
        return ResponseEntity.ok(status);
    }

    @PostMapping("/claim")
    public ResponseEntity<Map<String, Object>> claimDailyLogin(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        String username = getUsernameFromAuth(auth);
        Map<String, Object> result = dailyLoginService.claimDailyLogin(username);

        if ((boolean) result.getOrDefault("success", false)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/checkin")
    public ResponseEntity<Map<String, Object>> checkIn(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        String username = getUsernameFromAuth(auth);
        Map<String, Object> result = dailyLoginService.checkIn(username);

        if ((boolean) result.getOrDefault("success", false)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @PostMapping("/claim-bonus")
    public ResponseEntity<Map<String, Object>> claimStreakBonus(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        String username = getUsernameFromAuth(auth);
        Map<String, Object> result = dailyLoginService.claimStreakBonus(username);

        if ((boolean) result.getOrDefault("success", false)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }

    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getLoginHistory(Authentication auth) {
        if (auth == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Chưa đăng nhập"));
        }

        String username = getUsernameFromAuth(auth);
        List<DailyLogin> history = dailyLoginService.getLoginHistory(username);

        Map<String, Object> response = new HashMap<>();
        response.put("history", history);
        response.put("totalPoints", dailyLoginService.calculateTotalPoints(username));

        return ResponseEntity.ok(response);
    }
}
