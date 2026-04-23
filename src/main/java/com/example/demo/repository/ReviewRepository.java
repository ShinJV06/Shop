package com.example.demo.repository;

import com.example.demo.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findAllByOrderByCreatedAtDesc();
    List<Review> findByProductSlugOrderByCreatedAtDesc(String productSlug);
    
    // Chỉ lấy review đã được duyệt
    List<Review> findByApprovedTrueOrderByCreatedAtDesc();
    List<Review> findByProductSlugAndApprovedTrueOrderByCreatedAtDesc(String productSlug);
    
    // Lấy review chờ duyệt (cho admin)
    List<Review> findByApprovedFalseOrderByCreatedAtDesc();
    List<Review> findByModerationStatusOrderByCreatedAtDesc(String status);
    
    // Đếm review theo trạng thái
    long countByApprovedTrue();
    long countByApprovedFalse();
    long countByModerationStatus(String status);
}
