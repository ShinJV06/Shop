package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "loot_box_history")
public class LootBoxHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long lootBoxId;

    @Column(nullable = false)
    private String lootBoxName;

    @Column(nullable = false)
    private String tier;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private Long rewardId;

    @Column(nullable = false)
    private String rewardName;

    @Column(nullable = false)
    private String rewardRarity;

    private boolean isWin; // Có nhận được account thật không

    private Long orderId; // Đơn hàng được tạo nếu isWin = true

    private String credentials; // Tài khoản được giao (nếu có)

    private Date createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
