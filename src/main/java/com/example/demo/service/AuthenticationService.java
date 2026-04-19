package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.Role;
import com.example.demo.exception.AccountNotFoundException;
import com.example.demo.exception.DuplicateEntity;
import com.example.demo.model.AccountResponse;
import com.example.demo.model.ForgotPasswordRequest;
import com.example.demo.model.LoginRequest;
import com.example.demo.model.RegisterRequest;
import com.example.demo.model.ResetPasswordRequest;
import com.example.demo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
public class AuthenticationService {
    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender mailSender;

    public AccountResponse register(RegisterRequest request) {
        if (accountRepository.findAccountByUsername(request.getUsername()) != null) {
            throw new DuplicateEntity("Username already exists");
        }
        if (accountRepository.findAccountByEmail(request.getEmail()) != null) {
            throw new DuplicateEntity("Email already exists");
        }

        Account account = new Account();
        account.setUsername(request.getUsername());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setRole(Role.USER);
        account.setLocked(false);
        account.setCreatedAt(new Date());

        Account savedAccount = accountRepository.save(account);
        String token = tokenService.generateToken(savedAccount);
        return toAccountResponse(savedAccount, token);
    }

    public AccountResponse login(LoginRequest request) {
        Account account = accountRepository.findAccountByUsername(request.getUsername());
        if (account == null || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new AccountNotFoundException("Invalid username or password");
        }
        if (account.isLocked()) {
            throw new AccountNotFoundException("Tài khoản đã bị khóa. Liên hệ admin.");
        }
        if (account.getRole() == Role.BANNED) {
            throw new AccountNotFoundException("Tài khoản bị cấm.");
        }

        String token = tokenService.generateToken(account);
        return toAccountResponse(account, token);
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        Account account = accountRepository.findAccountByEmail(request.getEmail());
        if (account == null) {
            throw new AccountNotFoundException("Email does not exist");
        }

        String resetToken = UUID.randomUUID().toString();
        account.setResetToken(resetToken);
        account.setResetTokenExpiredAt(new Date(System.currentTimeMillis() + 1000L * 60 * 15));
        accountRepository.save(account);

        sendResetEmail(account.getEmail(), resetToken);
        return "Reset password token has been sent to your email";
    }

    public String resetPassword(ResetPasswordRequest request) {
        Account account = accountRepository.findAccountByResetToken(request.getToken());
        if (account == null) {
            throw new AccountNotFoundException("Invalid reset token");
        }

        if (account.getResetTokenExpiredAt() == null || account.getResetTokenExpiredAt().before(new Date())) {
            throw new AccountNotFoundException("Reset token has expired");
        }

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        account.setResetToken(null);
        account.setResetTokenExpiredAt(null);
        accountRepository.save(account);

        return "Password has been reset successfully";
    }

    public AccountResponse getProfileById(long id) {
        Account account = accountRepository.findAccountById(id);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
        return toAccountResponse(account, null);
    }

    private void sendResetEmail(String email, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Password reset");
        message.setText("Your reset password token is: " + resetToken + "\nToken will expire in 15 minutes.");
        mailSender.send(message);
    }

    private AccountResponse toAccountResponse(Account account, String token) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setUsername(account.getUsername());
        response.setEmail(account.getEmail());
        response.setPhone(account.getPhone());
        response.setRole(account.getRole());
        response.setToken(token);
        response.setCreatedAt(account.getCreatedAt());
        response.setWallet(account.getWallet());
        response.setProvider(account.getProvider());
        return response;
    }

    public void updateProfile(long userId, String phone) {
        Account account = accountRepository.findAccountById(userId);
        if (account == null) {
            throw new AccountNotFoundException("Account not found");
        }
        if (phone != null && !phone.isBlank()) {
            account.setPhone(phone.trim());
        }
        accountRepository.save(account);
    }
}



