package com.example.demo.model;

import com.example.demo.entity.Enum.Role;
import lombok.Data;

import java.util.Date;

@Data
public class AccountResponse {
    Long id;
    String username;
    String email;
    String phone;
    String token;
    Role role;
    Date createdAt;
    double wallet;
    String provider;
}
