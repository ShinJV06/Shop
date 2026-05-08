package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.Role;
import com.example.demo.model.FacebookUserResponse;
import com.example.demo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpSession;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/api/auth")
public class FacebookAuthController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${facebook.app.id}")
    private String facebookAppId;

    @Value("${facebook.app.secret}")
    private String facebookAppSecret;

    private static final String FACEBOOK_DEBUG_TOKEN_URL = "https://graph.facebook.com/debug_token";
    private static final String FACEBOOK_USER_INFO_URL = "https://graph.facebook.com/me";

    @PostMapping("/facebook")
    public String handleFacebookCallback(@RequestParam("accessToken") String accessToken, HttpSession session) {
        try {
            RestTemplate restTemplate = new RestTemplate();

            // Verify token with Facebook
            String debugUrl = FACEBOOK_DEBUG_TOKEN_URL + "?input_token=" + accessToken + "&access_token=" + facebookAppId + "|" + facebookAppSecret;
            ResponseEntity<Map> debugResponse = restTemplate.getForEntity(debugUrl, Map.class);

            if (debugResponse.getStatusCode() != HttpStatus.OK || debugResponse.getBody() == null) {
                return "redirect:/auth/login?error=facebook_verification_failed";
            }

            Map debugBody = debugResponse.getBody();
            Map data = (Map) debugBody.get("data");
            if (data == null || !Boolean.TRUE.equals(data.get("is_valid"))) {
                return "redirect:/auth/login?error=facebook_invalid_token";
            }

            // Get user info from Facebook
            String userInfoUrl = FACEBOOK_USER_INFO_URL + "?access_token=" + accessToken + "&fields=id,name,email,picture";
            ResponseEntity<FacebookUserResponse> userResponse = restTemplate.getForEntity(
                userInfoUrl, FacebookUserResponse.class
            );

            if (userResponse.getStatusCode() == HttpStatus.OK && userResponse.getBody() != null) {
                FacebookUserResponse fbUser = userResponse.getBody();
                
                String email = fbUser.getEmail();
                if (email == null || email.isEmpty()) {
                    email = fbUser.getId() + "@facebook.com";
                }
                
                Account account = accountRepository.findAccountByEmail(email);
                if (account == null) {
                    account = new Account();
                    account.setEmail(email);
                    account.setUsername("fb_" + (fbUser.getName() != null ? 
                        fbUser.getName().replaceAll("\\s+", "") : email.split("@")[0]));
                    account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    account.setRole(Role.USER);
                    account.setCreatedAt(new Date());
                    account.setLocked(false);
                    account.setWallet(0.0);
                    account.setProvider("facebook");
                    accountRepository.save(account);
                } else {
                    account.setProvider("facebook");
                    accountRepository.save(account);
                }

                session.setAttribute("userId", account.getId());
                session.setAttribute("username", account.getUsername());
                session.setAttribute("role", account.getRole().name());

                return "redirect:/";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        return "redirect:/auth/login?error=facebook_login_failed";
    }
}
