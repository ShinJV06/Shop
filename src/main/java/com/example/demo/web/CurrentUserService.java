package com.example.demo.web;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.Role;
import com.example.demo.repository.AccountRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    @Autowired
    private AccountRepository accountRepository;

    public Account requireUser(HttpSession session) {
        Long id = (Long) session.getAttribute("userId");
        if (id == null) {
            throw new UnauthorizedException("Cần đăng nhập.");
        }
        Account a = accountRepository.findAccountById(id);
        if (a == null) {
            throw new UnauthorizedException("Phiên không hợp lệ.");
        }
        return a;
    }

    public Account requireAdmin(HttpSession session) {
        Account a = requireUser(session);
        if (a.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Chỉ admin.");
        }
        return a;
    }

    public Account requireCtvOrAdmin(HttpSession session) {
        Account a = requireUser(session);
        if (a.getRole() != Role.ADMIN && a.getRole() != Role.CTV) {
            throw new UnauthorizedException("Chỉ admin hoặc CTV.");
        }
        return a;
    }

    public Account requireCtv(HttpSession session) {
        Account a = requireUser(session);
        if (a.getRole() != Role.CTV) {
            throw new UnauthorizedException("Chỉ CTV mới vào được trang này.");
        }
        return a;
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }
}
