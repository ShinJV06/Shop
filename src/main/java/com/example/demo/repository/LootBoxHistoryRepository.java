package com.example.demo.repository;

import com.example.demo.entity.LootBoxHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LootBoxHistoryRepository extends JpaRepository<LootBoxHistory, Long> {
    
    List<LootBoxHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<LootBoxHistory> findTop50ByOrderByCreatedAtDesc();
    
    @Query("SELECT l FROM LootBoxHistory l ORDER BY l.createdAt DESC")
    List<LootBoxHistory> findRecentHistory(Pageable pageable);
    
    long countByUserId(Long userId);
    
    long countByUserIdAndIsWinTrue(Long userId);
}
