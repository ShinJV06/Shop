package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "loot_box_rewards")
public class LootBoxReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loot_box_id", nullable = false)
    private LootBox lootBox;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String rarity; // COMMON, RARE, LEGENDARY

    private int chance; // 1-100

    @Column(length = 500)
    private String description;

    private int quantity; // Số lượng account trong kho

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    public String getProductNameSnapshot() {
        if (productNameSnapshot != null) return productNameSnapshot;
        if (product != null) return product.getName();
        return name;
    }
}
