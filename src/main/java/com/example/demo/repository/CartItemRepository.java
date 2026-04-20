package com.example.demo.repository;

import com.example.demo.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    
    List<CartItem> findByAccountIdOrderByAddedAtDesc(Long accountId);
    
    Optional<CartItem> findByAccountIdAndProductSlug(Long accountId, String productSlug);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.accountId = :accountId AND c.productSlug = :productSlug")
    void deleteByAccountIdAndProductSlug(@Param("accountId") Long accountId, @Param("productSlug") String productSlug);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.accountId = :accountId")
    void deleteAllByAccountId(@Param("accountId") Long accountId);
    
    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.accountId = :accountId AND c.productSlug IN :slugs")
    void deleteByAccountIdAndProductSlugIn(@Param("accountId") Long accountId, @Param("slugs") List<String> slugs);
}
