package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.TransactionAction;
import com.example.demo.entity.TransactionLogEntry;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.TransactionLogEntryRepository;
import com.example.demo.service.ShopCatalogService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
@RequestMapping("/wallet")
public class WalletController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionLogEntryRepository transactionLogEntryRepository;

    @Autowired
    private ShopCatalogService shopCatalogService;

    @GetMapping
    public String walletPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/auth/login";
        }
        Account account = accountRepository.findAccountById(userId);
        if (account == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("wallet", shopCatalogService.formatPrice(account.getWallet()));
        model.addAttribute("walletRaw", account.getWallet());

        List<TransactionLogEntry> logs = transactionLogEntryRepository.findByActorAccountIdOrderByIdDesc(userId);
        model.addAttribute("transactions", logs);

        return "wallet";
    }

    @PostMapping("/deposit")
    @ResponseBody
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> body, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Cần đăng nhập."));
        }

        Object amountObj = body.get("amount");
        double amount;
        if (amountObj instanceof Number) {
            amount = ((Number) amountObj).doubleValue();
        } else {
            try {
                amount = Double.parseDouble(amountObj.toString());
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Số tiền không hợp lệ."));
            }
        }

        if (amount <= 0) {
            return ResponseEntity.badRequest().body(Map.of("message", "Số tiền phải lớn hơn 0."));
        }

        Account account = accountRepository.findAccountById(userId);
        if (account == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy tài khoản."));
        }

        account.setWallet(account.getWallet() + amount);
        accountRepository.save(account);

        TransactionLogEntry log = new TransactionLogEntry();
        log.setActorAccountId(userId);
        log.setAmount(amount);
        log.setType("DEPOSIT");
        log.setAction(TransactionAction.DEPOSIT);
        log.setDetail("Nạp tiền qua QR Momo/ZaloBank");
        transactionLogEntryRepository.save(log);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("message", "Nạp tiền thành công!");
        res.put("newBalance", shopCatalogService.formatPrice(account.getWallet()));
        res.put("newBalanceRaw", account.getWallet());
        return ResponseEntity.ok(res);
    }

    @GetMapping("/top-depositors")
    @ResponseBody
    public ResponseEntity<?> topDepositors(HttpSession session) {
        List<Account> top = accountRepository.findTopDepositors(10);
        List<Map<String, Object>> result = new ArrayList<>();

        for (Account acc : top) {
            Double total = transactionLogEntryRepository.totalDepositsByAccountId(acc.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", acc.getId());
            item.put("username", acc.getUsername());
            item.put("totalDeposited", total != null ? total : 0.0);
            item.put("totalDepositedFormatted", shopCatalogService.formatPrice(total != null ? total : 0.0));
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }
}
