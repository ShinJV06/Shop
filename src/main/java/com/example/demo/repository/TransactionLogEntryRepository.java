package com.example.demo.repository;

import com.example.demo.entity.TransactionLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionLogEntryRepository extends JpaRepository<TransactionLogEntry, Long> {
    List<TransactionLogEntry> findByActorAccountIdOrderByIdDesc(Long accountId);

    @Query("SELECT SUM(t.amount) FROM TransactionLogEntry t WHERE t.actorAccountId = :accountId AND t.type = 'DEPOSIT'")
    Double totalDepositsByAccountId(Long accountId);

    List<TransactionLogEntry> findByAction(com.example.demo.entity.Enum.TransactionAction action);

    List<TransactionLogEntry> findByActionAndCreatedAtBetween(
            com.example.demo.entity.Enum.TransactionAction action,
            java.util.Date from,
            java.util.Date to);
}
