package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.ShopOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class NotificationService {

    @Autowired
    private EmailNotificationService emailService;

    @Autowired
    private SmsNotificationService smsService;

    @Autowired
    private AccountService accountService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Async
    public void notifyOrderCreated(ShopOrder order) {
        Optional<Account> buyerOpt = accountService.findById(order.getBuyerId());
        if (buyerOpt.isEmpty()) return;

        Account buyer = buyerOpt.get();

        emailService.sendOrderConfirmation(order, buyer.getUsername(), buyer.getEmail());

        if (buyer.getPhone() != null && !buyer.getPhone().isBlank()) {
            smsService.sendOrderCreatedSms(
                buyer.getPhone(),
                String.valueOf(order.getId()),
                formatMoney(order.getTotalAmount())
            );
        }
    }

    @Async
    public void notifyOrderPaid(ShopOrder order) {
        Optional<Account> buyerOpt = accountService.findById(order.getBuyerId());
        if (buyerOpt.isEmpty()) return;

        Account buyer = buyerOpt.get();

        emailService.sendOrderPaidNotification(order, buyer.getUsername(), buyer.getEmail());

        if (buyer.getPhone() != null && !buyer.getPhone().isBlank()) {
            smsService.sendOrderPaidSms(buyer.getPhone(), String.valueOf(order.getId()));
        }
    }

    @Async
    public void notifyOrderRefunded(ShopOrder order) {
        Optional<Account> buyerOpt = accountService.findById(order.getBuyerId());
        if (buyerOpt.isEmpty()) return;

        Account buyer = buyerOpt.get();

        emailService.sendOrderRefundedNotification(order, buyer.getUsername(), buyer.getEmail());

        if (buyer.getPhone() != null && !buyer.getPhone().isBlank()) {
            smsService.sendRefundSms(
                buyer.getPhone(),
                String.valueOf(order.getId()),
                formatMoney(order.getTotalAmount())
            );
        }
    }

    @Async
    public void notifyWelcome(Account account) {
        emailService.sendWelcomeEmail(
            account.getUsername(),
            account.getEmail(),
            DATE_FORMAT.format(account.getCreatedAt())
        );
    }

    @Async
    public void notifyPasswordReset(Account account, String ipAddress) {
        emailService.sendPasswordResetEmail(
            account.getUsername(),
            account.getEmail(),
            account.getResetToken(),
            ipAddress
        );

        if (account.getPhone() != null && !account.getPhone().isBlank()) {
            smsService.sendPasswordResetSms(account.getPhone(), account.getResetToken());
        }
    }

    @Async
    public void notifyWithdrawalRequest(Account account, long amount,
            String method, String bankName, String bankAccount) {
        emailService.sendWithdrawalRequestEmail(
            account.getUsername(),
            account.getEmail(),
            amount,
            method,
            bankName,
            bankAccount
        );
    }

    @Async
    public void notifyWithdrawalComplete(Account account, long amount,
            String method, String bankName, String bankAccount) {
        emailService.sendWithdrawalCompleteEmail(
            account.getUsername(),
            account.getEmail(),
            amount,
            method,
            bankName,
            bankAccount
        );

        if (account.getPhone() != null && !account.getPhone().isBlank()) {
            smsService.sendWithdrawalSms(account.getPhone(), formatMoney(amount));
        }
    }

    @Async
    public void notifyDepositConfirmation(Account account, long amount,
            String paymentMethod, String transactionId, String transferContent,
            String bankAccount, String bankName) {
        emailService.sendDepositConfirmationEmail(
            account.getUsername(),
            account.getEmail(),
            amount,
            paymentMethod,
            transactionId,
            transferContent,
            bankAccount,
            bankName
        );
    }

    @Async
    public void notifyAccountApproved(Account seller, String productName) {
        emailService.sendAccountApprovedEmail(
            seller.getUsername(),
            seller.getEmail(),
            productName
        );
    }

    @Async
    public void notifyAccountRejected(Account seller, String productName, String reason) {
        emailService.sendAccountRejectedEmail(
            seller.getUsername(),
            seller.getEmail(),
            productName,
            reason
        );
    }

    @Async
    public void sendOtp(String phone, String otp) {
        smsService.sendOtp(phone, otp);
    }

    @Async
    public void sendOrderConfirmationOtp(String phone, String orderId, String otp) {
        smsService.sendOrderOtp(phone, orderId, otp);
    }

    private String formatMoney(long amount) {
        return String.format("%,d", amount).replace(",", ".");
    }
}
