package com.example.demo.repository;

import com.example.demo.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySlugIgnoreCase(String slug);

    List<Product> findByVisibleTrueOrderByIdAsc();
}
