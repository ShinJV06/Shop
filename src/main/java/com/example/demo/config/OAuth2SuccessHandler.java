package com.example.demo.config;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.Role;
import com.example.demo.repository.AccountRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    @Autowired
    private AccountRepository accountRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oauth2User = oauthToken.getPrincipal();
        
        String provider = oauthToken.getAuthorizedClientRegistrationId();
        String email = null;
        String name = null;
        String username = null;

        if ("google".equals(provider)) {
            Map<String, Object> attributes = oauth2User.getAttributes();
            email = (String) attributes.get("email");
            name = (String) attributes.get("name");
            username = "gg_" + email.split("@")[0];
        } else if ("facebook".equals(provider)) {
            Map<String, Object> attributes = oauth2User.getAttributes();
            email = (String) attributes.get("email");
            if (email == null) {
                email = (String) attributes.get("id") + "@facebook.com";
            }
            name = (String) attributes.get("name");
            username = "fb_" + (name != null ? name.replaceAll("\\s+", "") : email.split("@")[0]);
        }

        if (email != null) {
            Account account = accountRepository.findAccountByEmail(email);
            if (account == null) {
                account = new Account();
                account.setEmail(email);
                account.setUsername(username);
                account.setPassword(UUID.randomUUID().toString());
                account.setRole(Role.USER);
                account.setCreatedAt(new Date());
                account.setLocked(false);
                account.setWallet(0.0);
                account.setProvider(provider);
                accountRepository.save(account);
            } else {
                account.setProvider(provider);
                accountRepository.save(account);
            }
            request.getSession().setAttribute("userId", account.getId());
            request.getSession().setAttribute("username", account.getUsername());
            request.getSession().setAttribute("role", account.getRole().name());
        }

        response.sendRedirect("/");
    }
}
