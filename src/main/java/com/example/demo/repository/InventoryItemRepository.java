package com.example.demo.repository;

import com.example.demo.entity.InventoryItem;
import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    List<InventoryItem> findAllByOrderByIdDesc();

    boolean existsByCredentialHash(String credentialHash);

    long countByProduct_IdAndStatusAndModerationStatusAndHiddenFalse(
            Long productId,
            InventoryItemStatus status,
            ModerationStatus moderationStatus
    );

    long countByStatus(InventoryItemStatus status);

    long countByModerationStatus(ModerationStatus moderationStatus);

    long countByStatusAndModerationStatusAndHiddenFalse(
            InventoryItemStatus status,
            ModerationStatus moderationStatus
    );

    List<InventoryItem> findByModerationStatusOrderByIdDesc(ModerationStatus moderationStatus);

    List<InventoryItem> findTop6ByModerationStatusOrderByIdDesc(ModerationStatus moderationStatus);

    List<InventoryItem> findByModerationStatusAndHiddenFalseOrderByIdDesc(ModerationStatus moderationStatus);

    @Query(value = """
            SELECT * FROM inventory_items i
            WHERE i.product_id = :productId
              AND i.status = 'AVAILABLE'
              AND i.moderation_status = 'APPROVED'
              AND i.hidden = false
            ORDER BY RAND() LIMIT 1
            """, nativeQuery = true)
    Optional<InventoryItem> pickRandomAvailable(@Param("productId") Long productId);
}
