package com.example.demo.repository;

import com.example.demo.entity.LootBox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LootBoxRepository extends JpaRepository<LootBox, Long> {
    
    List<LootBox> findByActiveTrueOrderByPriceAsc();
    
    Optional<LootBox> findByTier(String tier);
    
    @Query("SELECT l FROM LootBox l LEFT JOIN FETCH l.rewards WHERE l.active = true ORDER BY l.price ASC")
    List<LootBox> findAllActiveWithRewards();
    
    @Query("SELECT l FROM LootBox l LEFT JOIN FETCH l.rewards WHERE l.id = :id")
    Optional<LootBox> findByIdWithRewards(Long id);
}
