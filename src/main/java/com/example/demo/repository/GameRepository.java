package com.example.demo.repository;

import com.example.demo.entity.Game;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    Optional<Game> findBySlug(String slug);

    @Query("SELECT g FROM Game g WHERE g.visible = true ORDER BY g.displayOrder ASC, g.id ASC")
    List<Game> findAllVisibleOrderByDisplayOrder();

    @Query("SELECT g FROM Game g WHERE g.visible = true AND LOWER(g.slug) = LOWER(:slug)")
    Optional<Game> findBySlugIgnoreCase(String slug);
}
