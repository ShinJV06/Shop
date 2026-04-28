package com.example.demo.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "mystery_bags")
public class MysteryBag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int price;

    @Column(length = 500)
    private String description;

    private boolean active = true;

    @Column(name = "image_url")
    private String imageUrl;

    @OneToMany(mappedBy = "mysteryBag", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({"mysteryBag", "hibernateLazyInitializer", "handler"})
    private List<MysteryBagReward> rewards = new ArrayList<>();
}
