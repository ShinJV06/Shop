package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "mystery_bag_history")
public class MysteryBagHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @Column(name = "mystery_bag_id")
    private Long mysteryBagId;

    @Column(name = "mystery_bag_name")
    private String mysteryBagName;

    @Column(name = "reward_id")
    private Long rewardId;

    @Column(name = "reward_name")
    private String rewardName;

    @Column(name = "reward_rarity")
    private String rewardRarity;

    private boolean win;

    @Column(name = "credentials")
    private String credentials;

    private int price;

    @Column(name = "created_at")
    private Date createdAt = new Date();
}
