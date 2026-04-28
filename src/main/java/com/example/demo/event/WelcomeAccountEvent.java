package com.example.demo.event;

import com.example.demo.entity.Account;
import org.springframework.context.ApplicationEvent;

public class WelcomeAccountEvent extends ApplicationEvent {
    private final Account account;

    public WelcomeAccountEvent(Account account) {
        super(account);
        this.account = account;
    }

    public Account getAccount() {
        return account;
    }
}
