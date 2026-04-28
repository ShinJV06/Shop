package com.example.demo.repository;

import com.example.demo.entity.MysteryBagHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MysteryBagHistoryRepository extends JpaRepository<MysteryBagHistory, Long> {
    
    List<MysteryBagHistory> findByUserIdOrderByCreatedAtDesc(Long userId);
    
    List<MysteryBagHistory> findTop20ByOrderByCreatedAtDesc();
    
    List<MysteryBagHistory> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
