package com.example.demo.listener;

import com.example.demo.event.WelcomeAccountEvent;
import com.example.demo.service.EmailNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;

@Component
public class AccountEventListener {

    @Autowired
    private EmailNotificationService emailService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    @Async
    @EventListener
    public void handleWelcomeEvent(WelcomeAccountEvent event) {
        emailService.sendWelcomeEmail(
            event.getAccount().getUsername(),
            event.getAccount().getEmail(),
            DATE_FORMAT.format(event.getAccount().getCreatedAt())
        );
    }
}
