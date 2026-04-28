package com.example.demo.repository;

import com.example.demo.entity.MysteryBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MysteryBagRepository extends JpaRepository<MysteryBag, Long> {
    
    @Query("SELECT m FROM MysteryBag m WHERE m.active = true")
    List<MysteryBag> findAllActive();
}
