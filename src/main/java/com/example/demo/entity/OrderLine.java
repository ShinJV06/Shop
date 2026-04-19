package com.example.demo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private ShopOrder order;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false, length = 200)
    private String productNameSnapshot;

    @Column(nullable = false)
    private long unitPrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_inventory_id")
    private InventoryItem assignedInventory;

    @Column(length = 8000)
    private String deliveredCredentials;

    public void attachOrder(ShopOrder o) {
        this.order = o;
        o.getLines().add(this);
    }
}
