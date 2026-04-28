package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "mystery_bag_rewards")
public class MysteryBagReward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mystery_bag_id", nullable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private MysteryBag mysteryBag;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String rarity;

    private int chance;

    @Column(length = 500)
    private String description;

    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Product product;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    public String getProductNameSnapshot() {
        if (productNameSnapshot != null) return productNameSnapshot;
        if (product != null) return product.getName();
        return name;
    }
}
