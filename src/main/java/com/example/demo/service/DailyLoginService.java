package com.example.demo.service;

import com.example.demo.entity.Account;
import com.example.demo.entity.DailyLogin;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.DailyLoginRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DailyLoginService {

    private static final Logger logger = LoggerFactory.getLogger(DailyLoginService.class);

    @Autowired
    private DailyLoginRepository dailyLoginRepository;

    @Autowired
    private AccountRepository accountRepository;

    private static final int POINTS_PER_DAY = 10;
    private static final int STREAK_BONUS = 70;
    private static final int STREAK_DAYS_FOR_BONUS = 7;

    public Map<String, Object> checkDailyLogin(String username) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        boolean alreadyLoggedInToday = dailyLoginRepository
            .findByUsernameAndDateRange(username, startOfDay, endOfDay)
            .isPresent();

        Optional<DailyLogin> lastLogin = dailyLoginRepository.findFirstByUsernameOrderByLoginDateDesc(username);

        int currentStreak = 0;
        int totalPoints = 0;
        int todayPoints = 0;
        boolean canClaimStreakBonus = false;
        boolean streakBonusClaimed = false;
        int remainingDaysForBonus = 0;

        if (lastLogin.isPresent()) {
            DailyLogin last = lastLogin.get();
            currentStreak = last.getStreakDay();
            totalPoints = calculateTotalPoints(username);

            if (alreadyLoggedInToday) {
                todayPoints = last.getPointsEarned();
                result.put("alreadyClaimedToday", true);
            } else {
                result.put("alreadyClaimedToday", false);
            }

            if (currentStreak >= STREAK_DAYS_FOR_BONUS && !last.isBonusClaimed()) {
                canClaimStreakBonus = true;
                remainingDaysForBonus = 0;
            } else if (currentStreak >= STREAK_DAYS_FOR_BONUS && last.isBonusClaimed()) {
                streakBonusClaimed = true;
                remainingDaysForBonus = 0;
            } else {
                remainingDaysForBonus = STREAK_DAYS_FOR_BONUS - currentStreak;
            }
        } else {
            result.put("alreadyClaimedToday", false);
            remainingDaysForBonus = STREAK_DAYS_FOR_BONUS;
        }

        result.put("alreadyLoggedInToday", alreadyLoggedInToday);
        result.put("currentStreak", currentStreak);
        result.put("totalPoints", totalPoints);
        result.put("todayPoints", todayPoints);
        result.put("canClaimStreakBonus", canClaimStreakBonus);
        result.put("streakBonusClaimed", streakBonusClaimed);
        result.put("remainingDaysForBonus", remainingDaysForBonus);
        result.put("streakDaysForBonus", STREAK_DAYS_FOR_BONUS);

        return result;
    }

    @Transactional
    public Map<String, Object> checkIn(String username) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        logger.info("=== CHECKIN START === Username: '{}'", username);

        // Debug: Kiểm tra account có tồn tại không
        Account acc = accountRepository.findAccountByUsername(username);
        if (acc == null) {
            logger.error("Account NOT FOUND for username: '{}'", username);
            result.put("success", false);
            result.put("message", "Không tìm thấy tài khoản!");
            return result;
        }
        logger.info("Account found! ID: {}, Wallet before: {}", acc.getId(), acc.getWallet());

        // Kiểm tra đã check-in hôm nay chưa
        Optional<DailyLogin> existingLogin = dailyLoginRepository.findByUsernameAndDateRange(username, startOfDay, endOfDay);
        if (existingLogin.isPresent()) {
            result.put("success", false);
            result.put("message", "Bạn đã điểm danh hôm nay rồi!");
            return result;
        }

        // Tính streak
        Optional<DailyLogin> lastLogin = dailyLoginRepository.findFirstByUsernameOrderByLoginDateDesc(username);
        int newStreak = 1;

        if (lastLogin.isPresent()) {
            DailyLogin last = lastLogin.get();
            LocalDate lastDate = last.getLoginDate().toLocalDate();
            LocalDate today = now.toLocalDate();
            long daysDiff = ChronoUnit.DAYS.between(lastDate, today);

            if (daysDiff == 1) {
                newStreak = last.getStreakDay() + 1;
            } else if (daysDiff == 0) {
                result.put("success", false);
                result.put("message", "Bạn đã điểm danh hôm nay rồi!");
                return result;
            }
        }

        // Tính tiền: streak * 10đ
        int moneyEarned = newStreak * POINTS_PER_DAY;
        boolean streakBonusReceived = false;
        int streakBonusAmount = 0;

        // Tạo record check-in
        DailyLogin newLogin = new DailyLogin(username, now, newStreak);
        newLogin.setPointsEarned(moneyEarned);
        newLogin.setBonusClaimed(false);
        dailyLoginRepository.save(newLogin);
        logger.info("DailyLogin saved with points: {}", moneyEarned);

        // Cộng tiền vào ví - UPDATE TRỰC TIẾP
        double walletBefore = acc.getWallet();
        acc.setWallet(walletBefore + moneyEarned);
        accountRepository.save(acc);
        logger.info("Wallet updated from {} to {}", walletBefore, acc.getWallet());

        // Bonus thêm nếu đủ 7 ngày liên tiếp
        if (newStreak >= STREAK_DAYS_FOR_BONUS) {
            streakBonusAmount = STREAK_BONUS;
            double newWallet = acc.getWallet() + streakBonusAmount;
            acc.setWallet(newWallet);
            accountRepository.save(acc);
            logger.info("Streak bonus added. Wallet now: {}", acc.getWallet());
            streakBonusReceived = true;

            newLogin.setBonusClaimed(true);
            dailyLoginRepository.save(newLogin);
        }

        result.put("success", true);
        result.put("message", "Điểm danh thành công! Streak: " + newStreak + " ngày");
        result.put("newStreak", newStreak);
        result.put("moneyEarned", moneyEarned);
        result.put("streakBonusReceived", streakBonusReceived);
        result.put("streakBonusAmount", streakBonusAmount);
        result.put("totalEarned", moneyEarned + streakBonusAmount);
        result.put("currentWallet", acc.getWallet());

        logger.info("=== CHECKIN END === Current wallet: {}", acc.getWallet());

        return result;
    }

    @Transactional
    public Map<String, Object> claimDailyLogin(String username) {
        Map<String, Object> result = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);

        Optional<DailyLogin> existingLogin = dailyLoginRepository.findByUsernameAndDateRange(username, startOfDay, endOfDay);
        if (existingLogin.isPresent()) {
            result.put("success", false);
            result.put("message", "Bạn đã nhận thưởng hôm nay rồi!");
            return result;
        }

        Optional<DailyLogin> lastLogin = dailyLoginRepository.findFirstByUsernameOrderByLoginDateDesc(username);
        int newStreak = 1;
        int pointsToday = POINTS_PER_DAY;
        boolean streakBonusReceived = false;

        if (lastLogin.isPresent()) {
            DailyLogin last = lastLogin.get();
            LocalDate lastDate = last.getLoginDate().toLocalDate();
            LocalDate today = now.toLocalDate();
            long daysDiff = ChronoUnit.DAYS.between(lastDate, today);

            if (daysDiff == 1) {
                newStreak = last.getStreakDay() + 1;
            } else if (daysDiff == 0) {
                result.put("success", false);
                result.put("message", "Bạn đã nhận thưởng hôm nay rồi!");
                return result;
            }
        }

        DailyLogin newLogin = new DailyLogin(username, now, newStreak);
        newLogin.setPointsEarned(pointsToday);
        newLogin.setBonusClaimed(false);
        dailyLoginRepository.save(newLogin);

        // Cộng tiền vào ví
        accountRepository.addToWallet(username, pointsToday);

        if (newStreak == STREAK_DAYS_FOR_BONUS) {
            accountRepository.addToWallet(username, STREAK_BONUS);
            streakBonusReceived = true;
        }

        int totalPoints = calculateTotalPoints(username);

        result.put("success", true);
        result.put("message", streakBonusReceived
            ? "Chúc mừng! Bạn đã đạt 7 ngày đăng nhập liên tục! Nhận ngay 70 đồng!"
            : "Điểm tích lũy hôm nay: " + pointsToday + " điểm");
        result.put("pointsEarned", pointsToday);
        result.put("newStreak", newStreak);
        result.put("totalPoints", totalPoints);
        result.put("streakBonusReceived", streakBonusReceived);
        result.put("streakBonusAmount", streakBonusReceived ? STREAK_BONUS : 0);

        return result;
    }

    @Transactional
    public Map<String, Object> claimStreakBonus(String username) {
        Map<String, Object> result = new HashMap<>();

        Optional<DailyLogin> lastLogin = dailyLoginRepository.findFirstByUsernameOrderByLoginDateDesc(username);
        if (lastLogin.isEmpty()) {
            result.put("success", false);
            result.put("message", "Không tìm thấy lịch sử đăng nhập!");
            return result;
        }

        DailyLogin last = lastLogin.get();
        if (last.getStreakDay() < STREAK_DAYS_FOR_BONUS) {
            result.put("success", false);
            result.put("message", "Bạn chưa đạt đủ 7 ngày đăng nhập liên tục!");
            return result;
        }

        if (last.isBonusClaimed()) {
            result.put("success", false);
            result.put("message", "Bạn đã nhận thưởng 7 ngày rồi!");
            return result;
        }

        accountRepository.addToWallet(username, STREAK_BONUS);

        last.setBonusClaimed(true);
        dailyLoginRepository.save(last);

        result.put("success", true);
        result.put("message", "Chúc mừng! Bạn nhận được 70 đồng!");
        result.put("bonusAmount", STREAK_BONUS);

        return result;
    }

    public int calculateTotalPoints(String username) {
        List<DailyLogin> logins = dailyLoginRepository.findByUsernameOrderByLoginDateDesc(username);
        int total = 0;
        for (DailyLogin login : logins) {
            total += login.getPointsEarned();
        }
        return total;
    }

    public List<DailyLogin> getLoginHistory(String username) {
        return dailyLoginRepository.findByUsernameOrderByLoginDateDesc(username);
    }
}
