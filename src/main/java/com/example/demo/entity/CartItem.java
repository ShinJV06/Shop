package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "cart_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"account_id", "product_slug"})
})
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "account_id", nullable = false)
    Long accountId;

    @Column(name = "product_slug", nullable = false, length = 255)
    String productSlug;

    @Column(nullable = false)
    Integer quantity = 1;

    @Column(name = "added_at", nullable = false)
    LocalDateTime addedAt = LocalDateTime.now();

    @PrePersist
    void prePersist() {
        if (addedAt == null) {
            addedAt = LocalDateTime.now();
        }
        if (quantity == null || quantity < 1) {
            quantity = 1;
        }
    }
}
