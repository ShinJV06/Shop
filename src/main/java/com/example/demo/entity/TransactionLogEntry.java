package com.example.demo.entity;

import com.example.demo.entity.Enum.TransactionAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@Entity
@Table(name = "transaction_logs")
public class TransactionLogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "actor_account_id")
    private Long actorAccountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TransactionAction action;

    @Column(length = 4000)
    private String detail;

    @Column(nullable = false)
    private Date createdAt = new Date();

    @Column
    private Double amount;

    @Column(length = 32)
    private String type; // DEPOSIT, WITHDRAW, PURCHASE
}
