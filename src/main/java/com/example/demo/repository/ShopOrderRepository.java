package com.example.demo.repository;

import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.entity.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    List<ShopOrder> findAllByOrderByIdDesc();

    List<ShopOrder> findByBuyerIdOrderByIdDesc(Long buyerId);

    @Query("SELECT COALESCE(SUM(o.totalAmount),0) FROM ShopOrder o WHERE o.status = 'PAID' AND o.paidAt >= :from AND o.paidAt < :to")
    long sumPaidRevenueBetween(@Param("from") Date from, @Param("to") Date to);

    long countByCreatedAtAfter(Date from);

    long countByStatus(OrderStatus status);

    List<ShopOrder> findTop8ByStatusOrderByIdDesc(OrderStatus status);

    List<ShopOrder> findTop5ByBuyerIdAndStatusOrderByIdDesc(Long buyerId, OrderStatus status);
}
