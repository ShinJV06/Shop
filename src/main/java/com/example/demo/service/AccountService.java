package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.repository.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Optional<Account> findById(Long id) {
        return accountRepository.findById(id);
    }

    public Account findAccountById(Long id) {
        return accountRepository.findAccountById(id);
    }

    public Optional<Account> findByEmail(String email) {
        return Optional.ofNullable(accountRepository.findAccountByEmail(email));
    }

    public Optional<Account> findByUsername(String username) {
        return Optional.ofNullable(accountRepository.findAccountByUsername(username));
    }
}
