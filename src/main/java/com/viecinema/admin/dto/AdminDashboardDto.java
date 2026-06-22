package com.viecinema.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO cho Admin Dashboard Overview.
 * Chứa thống kê tổng quan và dữ liệu biểu đồ.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardDto {

    // Thống kê tổng quan
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private long deletedUsers;
    private long newUsersToday;
    private long newUsersThisMonth;
    private double activePercentage;
    private double bannedPercentage;

    // Phân bổ role
    private Map<String, Long> roleDistribution;

    // Biểu đồ đăng ký theo ngày (30 ngày gần nhất)
    private List<DailyRegistrationStat> dailyRegistrations;

    // Biểu đồ đăng ký theo tháng (12 tháng gần nhất)
    private List<MonthlyRegistrationStat> monthlyRegistrations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRegistrationStat {
        @JsonFormat(pattern = "yyyy-MM-dd")
        private String date;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MonthlyRegistrationStat {
        private int year;
        private int month;
        private long count;
    }
}
