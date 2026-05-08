package com.example.demo.repository;

import com.example.demo.entity.DepositRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepositRequestRepository extends JpaRepository<DepositRequest, Long> {

    Optional<DepositRequest> findByOrderId(String orderId);

    Optional<DepositRequest> findByTransactionId(String transactionId);

    List<DepositRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<DepositRequest> findByStatus(DepositRequest.DepositStatus status);

    List<DepositRequest> findByUserIdAndStatus(Long userId, DepositRequest.DepositStatus status);

    boolean existsByOrderId(String orderId);
}
