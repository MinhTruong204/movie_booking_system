package com.viecinema.user.service;

import com.viecinema.user.dto.UserStatistics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserStatisticsService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Lấy thống kê tổng hợp của user
     */
    public UserStatistics getUserStatistics(Integer userId) {
        log.debug("Fetching statistics for user ID: {}", userId);

        String sql = """
                SELECT 
                    COALESCE(u.total_spent, 0) as total_spent,
                    COALESCE(COUNT(DISTINCT b.booking_id), 0) as total_bookings,
                    COALESCE(SUM(CASE WHEN b.status = 'paid' THEN 1 ELSE 0 END), 0) as total_tickets,
                    COALESCE(COUNT(DISTINCT mr.review_id), 0) as total_reviews,
                    COALESCE(COUNT(DISTINCT uw.watchlist_id), 0) as watchlist_count,
                    COALESCE(SUM(CASE 
                        WHEN YEAR(b.created_at) = YEAR(CURDATE()) 
                        AND MONTH(b.created_at) = MONTH(CURDATE()) 
                        AND b.status = 'paid'
                        THEN 1 ELSE 0 
                    END), 0) as bookings_this_month,
                    COALESCE(SUM(CASE 
                        WHEN YEAR(b.created_at) = YEAR(CURDATE()) 
                        AND MONTH(b.created_at) = MONTH(CURDATE())
                        AND b.status = 'paid'
                        THEN b.final_amount ELSE 0 
                    END), 0) as spent_this_month
                FROM users u
                LEFT JOIN bookings b ON u.user_id = b.user_id AND b.deleted_at IS NULL
                LEFT JOIN movie_reviews mr ON u.user_id = mr.user_id AND mr.deleted_at IS NULL
                LEFT JOIN user_watchlist uw ON u.user_id = uw.user_id
                WHERE u.user_id = ?
                GROUP BY u.user_id, u.total_spent
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                        UserStatistics.builder()
                                .totalSpent(rs.getBigDecimal("total_spent"))
                                .totalBookings(rs.getInt("total_bookings"))
                                .totalTickets(rs.getInt("total_tickets"))
                                .totalReviews(rs.getInt("total_reviews"))
                                .watchlistCount(rs.getInt("watchlist_count"))
                                .bookingsThisMonth(rs.getInt("bookings_this_month"))
                                .spentThisMonth(rs.getBigDecimal("spent_this_month"))
                                .build(),
                userId
        );
    }
}
