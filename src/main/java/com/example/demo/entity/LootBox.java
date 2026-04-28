package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "loot_boxes")
public class LootBox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tier; // COMMON, RARE, LEGENDARY

    @Column(nullable = false)
    private int price;

    @Column(length = 500)
    private String description;

    private boolean active = true;

    @OneToMany(mappedBy = "lootBox", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LootBoxReward> rewards = new ArrayList<>();
}
