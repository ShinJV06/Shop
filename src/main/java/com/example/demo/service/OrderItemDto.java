package com.example.demo.service;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemDto {
    private String name;
    private long price;
    private String username;
    private String password;
}
