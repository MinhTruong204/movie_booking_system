package com.viecinema.loyalty.entity;

import com.viecinema.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ánh xạ bảng loyalty_points_history.
 * Ghi lại từng thay đổi điểm: EARN (giao dịch), BONUS (review/sinh nhật),
 * REDEEM (tiêu điểm), EXPIRE (hết hạn), ADJUSTMENT (admin điều chỉnh).
 */
@Entity
@Table(name = "loyalty_points_history",
        uniqueConstraints = {
                // Chống duplicate: 1 booking chỉ EARN 1 lần
                @UniqueConstraint(name = "uk_booking_earn", columnNames = {"booking_id", "points_type"}),
                // Chống duplicate: 1 review chỉ BONUS 1 lần
                @UniqueConstraint(name = "uk_review_bonus", columnNames = {"review_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPointsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Liên kết đơn hàng — chỉ có khi type = EARN */
    @Column(name = "booking_id")
    private Integer bookingId;

    /** Liên kết review — chỉ có khi type = BONUS (review engagement) */
    @Column(name = "review_id")
    private Integer reviewId;

    @Column(name = "points_change", nullable = false)
    private Integer pointsChange;

    @Enumerated(EnumType.STRING)
    @Column(name = "points_type", nullable = false, length = 20)
    private PointsType pointsType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "old_balance", nullable = false)
    private Integer oldBalance;

    @Column(name = "new_balance", nullable = false)
    private Integer newBalance;

    @Column(name = "expires_at")
    private LocalDate expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum PointsType {
        EARN,       // Tích từ giao dịch thanh toán
        REDEEM,     // Tiêu điểm
        EXPIRE,     // Điểm hết hạn
        BONUS,      // Thưởng (review, sinh nhật, sự kiện)
        ADJUSTMENT  // Admin điều chỉnh thủ công
    }
}
