package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_login")
public class DailyLogin {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "username", nullable = false)
    private String username;
    
    @Column(name = "login_date", nullable = false)
    private LocalDateTime loginDate;
    
    @Column(name = "points_earned")
    private int pointsEarned = 10;
    
    @Column(name = "streak_day")
    private int streakDay;
    
    @Column(name = "bonus_claimed")
    private boolean bonusClaimed = false;
    
    public DailyLogin() {}
    
    public DailyLogin(String username, LocalDateTime loginDate, int streakDay) {
        this.username = username;
        this.loginDate = loginDate;
        this.streakDay = streakDay;
        this.pointsEarned = 10;
        this.bonusClaimed = false;
    }
    
    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public LocalDateTime getLoginDate() { return loginDate; }
    public void setLoginDate(LocalDateTime loginDate) { this.loginDate = loginDate; }
    
    public int getPointsEarned() { return pointsEarned; }
    public void setPointsEarned(int pointsEarned) { this.pointsEarned = pointsEarned; }
    
    public int getStreakDay() { return streakDay; }
    public void setStreakDay(int streakDay) { this.streakDay = streakDay; }
    
    public boolean isBonusClaimed() { return bonusClaimed; }
    public void setBonusClaimed(boolean bonusClaimed) { this.bonusClaimed = bonusClaimed; }
}
