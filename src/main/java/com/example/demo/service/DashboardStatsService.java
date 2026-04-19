package com.example.demo.service;

import com.example.demo.entity.Enum.InventoryItemStatus;
import com.example.demo.entity.Enum.ModerationStatus;
import com.example.demo.entity.Enum.OrderStatus;
import com.example.demo.repository.AccountRepository;
import com.example.demo.repository.InventoryItemRepository;
import com.example.demo.repository.ShopOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardStatsService {

    @Autowired
    private ShopOrderRepository shopOrderRepository;

    @Autowired
    private InventoryItemRepository inventoryItemRepository;

    @Autowired
    private AccountRepository accountRepository;

    public Map<String, Object> overview() {
        Date startOfDay = startOfToday();
        Date startOfMonth = startOfMonth();
        Date endTomorrow = endOfTomorrow();

        long revenueToday = shopOrderRepository.sumPaidRevenueBetween(startOfDay, endTomorrow);
        long revenueMonth = shopOrderRepository.sumPaidRevenueBetween(startOfMonth, endTomorrow);

        long ordersTotal = shopOrderRepository.count();
        long soldItems = inventoryItemRepository.countByStatus(InventoryItemStatus.SOLD);
        long stockAvailable = inventoryItemRepository.countByStatusAndModerationStatusAndHiddenFalse(
                InventoryItemStatus.AVAILABLE,
                ModerationStatus.APPROVED
        );

        long newUsersToday = accountRepository.countByCreatedAtAfter(startOfDay);
        long pendingModeration = inventoryItemRepository.countByModerationStatus(ModerationStatus.PENDING);

        Map<String, Object> m = new HashMap<>();
        m.put("revenueToday", revenueToday);
        m.put("revenueMonth", revenueMonth);
        m.put("ordersTotal", ordersTotal);
        m.put("ordersPendingPayment", shopOrderRepository.countByStatus(OrderStatus.PENDING_PAYMENT));
        m.put("soldItems", soldItems);
        m.put("stockAvailable", stockAvailable);
        m.put("newUsersToday", newUsersToday);
        m.put("pendingModeration", pendingModeration);

        List<String> chartLabels = new ArrayList<>();
        List<Long> chartValues = new ArrayList<>();
        SimpleDateFormat df = new SimpleDateFormat("dd/MM");
        for (int i = 6; i >= 0; i--) {
            Calendar day = Calendar.getInstance();
            day.setTime(new Date());
            day.set(Calendar.HOUR_OF_DAY, 0);
            day.set(Calendar.MINUTE, 0);
            day.set(Calendar.SECOND, 0);
            day.set(Calendar.MILLISECOND, 0);
            day.add(Calendar.DAY_OF_MONTH, -i);
            Date from = day.getTime();
            day.add(Calendar.DAY_OF_MONTH, 1);
            Date to = day.getTime();
            chartLabels.add(df.format(from));
            chartValues.add(shopOrderRepository.sumPaidRevenueBetween(from, to));
        }
        m.put("chartLabels", chartLabels);
        m.put("chartValues", chartValues);
        return m;
    }

    private static Date startOfToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date startOfMonth() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    private static Date endOfTomorrow() {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 59);
        c.set(Calendar.SECOND, 59);
        c.set(Calendar.MILLISECOND, 999);
        return c.getTime();
    }
}
