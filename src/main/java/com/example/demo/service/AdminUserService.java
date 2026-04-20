package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.Enum.Role;
import com.example.demo.entity.Enum.TransactionAction;
import com.example.demo.entity.ShopOrder;
import com.example.demo.entity.TransactionLogEntry;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.ShopOrderRepository;
import com.example.demo.repository.TransactionLogEntryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class AdminUserService {

    private static final char[] TEMP_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789abcdefghijkmnpqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private TransactionLogEntryRepository transactionLogEntryRepository;

    public List<Account> listAll() {
        return accountRepository.findAll();
    }

    @Transactional
    public void setLocked(long userId, boolean locked, long adminId) {
        Account acc = accountRepository.findAccountById(userId);
        if (acc == null) {
            throw new IllegalArgumentException("User không tồn tại.");
        }
        acc.setLocked(locked);
        accountRepository.save(acc);
        log(null, adminId, locked ? TransactionAction.USER_LOCKED : TransactionAction.USER_UNLOCKED,
                "User " + acc.getUsername() + " locked=" + locked);
    }

    @Transactional
    public void setRole(long userId, Role role, long adminId) {
        Account acc = accountRepository.findAccountById(userId);
        if (acc == null) {
            throw new IllegalArgumentException("User không tồn tại.");
        }
        acc.setRole(role);
        accountRepository.save(acc);
        log(null, adminId, TransactionAction.USER_ROLE_CHANGED, "Đổi role user " + acc.getUsername() + " -> " + role);
    }

    @Transactional
    public String resetPassword(long userId, long adminId) {
        Account acc = accountRepository.findAccountById(userId);
        if (acc == null) {
            throw new IllegalArgumentException("User không tồn tại.");
        }
        String temp = randomPassword(12);
        acc.setPassword(passwordEncoder.encode(temp));
        accountRepository.save(acc);
        log(null, adminId, TransactionAction.PASSWORD_RESET_BY_ADMIN, "Reset mật khẩu user " + acc.getUsername());
        return temp;
    }

    public List<ShopOrder> purchaseHistory(long userId) {
        return shopOrderRepository.findByBuyerIdAndStatusOrderByIdDesc(userId, OrderStatus.PAID);
    }

    private void log(Long orderId, Long actor, TransactionAction action, String detail) {
        TransactionLogEntry e = new TransactionLogEntry();
        e.setOrderId(orderId);
        e.setActorAccountId(actor);
        e.setAction(action);
        e.setDetail(detail);
        transactionLogEntryRepository.save(e);
    }

    private static String randomPassword(int len) {
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(TEMP_CHARS[RANDOM.nextInt(TEMP_CHARS.length)]);
        }
        return sb.toString();
    }
}
