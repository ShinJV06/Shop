package com.example.demo.repository;

import com.example.demo.entity.Account;
import com.example.demo.entity.Enum.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Account findAccountByUsername(String username);

    Account findAccountByEmail(String email);

    Account findAccountById(long id);

    Account findAccountByRole(Role role);

    Account findAccountByResetToken(String resetToken);

    long countByRole(Role role);

    long countByCreatedAtAfter(java.util.Date from);

    @Query(value = """
        SELECT a.* FROM account a
        INNER JOIN transaction_logs t ON a.id = t.actor_account_id
        WHERE t.type = 'DEPOSIT'
        AND MONTH(t.created_at) = MONTH(CURRENT_DATE)
        AND YEAR(t.created_at) = YEAR(CURRENT_DATE)
        GROUP BY a.id
        ORDER BY COALESCE(SUM(t.amount), 0) DESC
        LIMIT :limit
        """, nativeQuery = true)
    java.util.List<Account> findTopDepositors(int limit);
}
