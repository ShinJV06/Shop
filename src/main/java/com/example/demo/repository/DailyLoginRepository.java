package com.example.demo.repository;

import com.example.demo.entity.DailyLogin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLoginRepository extends JpaRepository<DailyLogin, Long> {
    
    List<DailyLogin> findByUsernameOrderByLoginDateDesc(String username);
    
    Optional<DailyLogin> findFirstByUsernameOrderByLoginDateDesc(String username);
    
    @Query("SELECT d FROM DailyLogin d WHERE d.username = :username AND d.loginDate >= :startDate AND d.loginDate < :endDate")
    Optional<DailyLogin> findByUsernameAndDateRange(
        @Param("username") String username,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    @Query("SELECT d FROM DailyLogin d WHERE d.username = :username ORDER BY d.loginDate DESC")
    List<DailyLogin> findRecentLogins(@Param("username") String username);
    
    @Query("SELECT COUNT(d) FROM DailyLogin d WHERE d.username = :username")
    int countTotalLogins(@Param("username") String username);
}
