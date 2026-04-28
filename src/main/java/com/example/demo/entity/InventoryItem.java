package com.example.demo.entity;

import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "inventory_items", indexes = {
        @Index(name = "idx_inv_product_status", columnList = "product_id,status,moderation_status,hidden")
})
public class InventoryItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    /** Game của acc — Genshin Impact, Liên Quân Mobile, v.v. */
    @Column(length = 120)
    private String game;

    /** AR Level / Rank — ví dụ: AR 58, Rank Kim Cương */
    @Column(length = 500)
    private String rankInfo;

    @Column(length = 1000)
    private String skinInfo;

    @Column(length = 2000)
    private String extraInfo;

    /** Giá riêng của acc này — null = dùng giá product */
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 8000)
    private String credentials;

    @Column(nullable = false, unique = true, length = 128, name = "credential_hash")
    private String credentialHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InventoryItemStatus status = InventoryItemStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Column(nullable = false)
    private boolean hidden = false;

    @Column(name = "submitted_by_id")
    private Long submittedById;

    @Column(name = "buyer_id")
    private Long buyerId;

    private Date soldAt;

    /** Ảnh minh hoạ acc (upload CTV / admin); có thể null với dữ liệu cũ — UI fallback ảnh gói. */
    @Column(length = 500)
    private String listingImagePath;

    private Date createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = new Date();
        }
    }
}
