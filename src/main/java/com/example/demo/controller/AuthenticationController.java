package com.example.demo.controller;


import com.example.demo.model.AccountResponse;
import com.example.demo.model.ForgotPasswordRequest;
import com.example.demo.model.LoginRequest;
import com.example.demo.model.RegisterRequest;
import com.example.demo.model.ResetPasswordRequest;
import com.example.demo.service.AuthenticationService;
import com.example.demo.service.ShopCatalogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;


@Controller
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private ShopCatalogService shopCatalogService;

    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<AccountResponse> registerApi(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authenticationService.register(request));
    }

    @PostMapping("/api/auth/login")
    @ResponseBody
    public ResponseEntity<AccountResponse> loginApi(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    @PostMapping("/api/auth/forgot-password")
    @ResponseBody
    public ResponseEntity<String> forgotPasswordApi(@Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(authenticationService.forgotPassword(request));
    }

    @PostMapping("/api/auth/reset-password")
    @ResponseBody
    public ResponseEntity<String> resetPasswordApi(@Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(authenticationService.resetPassword(request));
    }

    @GetMapping("/auth")
    public String authHome() {
        return "auth/home";
    }

    @GetMapping("/auth/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @GetMapping("/auth/spin")
    @ResponseBody // Quan trọng: Trả về dữ liệu JSON thay vì chuyển trang
    public ResponseEntity<Map<String, Object>> spinMiniGame(HttpSession session) {
        Map<String, Object> result = new HashMap<>();

        // 1. Kiểm tra đăng nhập (giống logic dòng 142 bạn đang có)
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2. Logic tính góc quay (Ví dụ vòng quay có 8 ô, mỗi ô 45 độ)
        int luckyIndex = (int) (Math.random() * 8); // Ngẫu nhiên từ 0-7
        int degree = luckyIndex * 45 + 3600; // 3600 là quay 10 vòng cho đẹp

        String prize = "Phần quà " + luckyIndex; // Bạn có thể map index này vào tên quà

        result.put("degree", degree);
        result.put("prize", prize);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/auth/register")
    public String register(@ModelAttribute RegisterRequest request, RedirectAttributes redirectAttributes) {
        try {
            authenticationService.register(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng ký thành công. Bạn có thể đăng nhập ngay.");
            return "redirect:/auth/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/register";
        }
    }

    @GetMapping("/auth/login")
    public String loginPage(Model model) {
        model.addAttribute("loginRequest", new LoginRequest());
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String login(@ModelAttribute LoginRequest request, RedirectAttributes redirectAttributes, HttpSession session) {
        try {
            AccountResponse accountResponse = authenticationService.login(request);
            session.setAttribute("userId", accountResponse.getId());
            session.setAttribute("username", accountResponse.getUsername());
            session.setAttribute("role", accountResponse.getRole() != null ? accountResponse.getRole().name() : "USER");
            session.setAttribute("token", accountResponse.getToken());
            session.setAttribute("wallet", shopCatalogService.formatPrice(accountResponse.getWallet()));
            session.setMaxInactiveInterval(10 * 60);
            redirectAttributes.addFlashAttribute("successMessage", "Đăng nhập thành công.");
            return "redirect:/";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/login";
        }
    }

    @GetMapping("/auth/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/auth/forgot-password")
    public String forgotPassword(String email, RedirectAttributes redirectAttributes) {
        try {
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail(email);
            authenticationService.forgotPassword(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đã gửi token đặt lại mật khẩu qua email của bạn.");
            return "redirect:/auth/reset-password";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/forgot-password";
        }
    }

    @GetMapping("/auth/reset-password")
    public String resetPasswordPage(Model model) {
        model.addAttribute("resetPasswordRequest", new ResetPasswordRequest());
        return "auth/reset-password";
    }

    @PostMapping("/auth/reset-password")
    public String resetPassword(@ModelAttribute ResetPasswordRequest request, RedirectAttributes redirectAttributes) {
        try {
            authenticationService.resetPassword(request);
            redirectAttributes.addFlashAttribute("successMessage", "Đổi mật khẩu thành công, hãy đăng nhập lại.");
            return "redirect:/auth/login";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/auth/reset-password";
        }
    }

    @GetMapping("/profile")
    public String profilePage(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            redirectAttributes.addFlashAttribute("errorMessage", "Bạn cần đăng nhập trước.");
            return "redirect:/auth/login";
        }

        long id = (Long) userId;
        model.addAttribute("profile", authenticationService.getProfileById(id));
        model.addAttribute("walletFormatted", shopCatalogService.formatPrice(authenticationService.getProfileById(id).getWallet()));
        return "profile";
    }

    @PostMapping("/profile/update")
    @ResponseBody
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body, HttpSession session) {
        Object userId = session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Cần đăng nhập."));
        }
        try {
            authenticationService.updateProfile((Long) userId, body.get("phone"));
            return ResponseEntity.ok(Map.of("message", "Cập nhật thông tin thành công!"));
        } catch (Exception ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PostMapping("/auth/logout")
    public String logout(HttpSession session, RedirectAttributes redirectAttributes) {
        session.invalidate();
        redirectAttributes.addFlashAttribute("successMessage", "Đăng xuất thành công.");
        return "redirect:/auth/login";
    }
}
