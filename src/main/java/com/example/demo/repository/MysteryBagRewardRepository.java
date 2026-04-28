package com.example.demo.repository;

import com.example.demo.entity.MysteryBagReward;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MysteryBagRewardRepository extends JpaRepository<MysteryBagReward, Long> {
    
    @Query("SELECT r FROM MysteryBagReward r WHERE r.mysteryBag.id = :bagId")
    List<MysteryBagReward> findByMysteryBagId(Long bagId);
}
