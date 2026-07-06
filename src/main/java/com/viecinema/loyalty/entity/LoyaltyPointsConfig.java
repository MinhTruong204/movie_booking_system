package com.viecinema.loyalty.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ánh xạ bảng loyalty_points_config.
 * Lưu cấu hình tỉ lệ tích điểm dưới dạng key-value.
 * Admin có thể thay đổi mà không cần redeploy.
 *
 * <pre>
 * Các key mặc định:
 *   EARN_RATE_PER_VND     = 0.0001   (1 điểm / 10.000 VND)
 *   REVIEW_BONUS_POINTS   = 20       (điểm thưởng review)
 *   BIRTHDAY_BONUS_POINTS = 100      (điểm thưởng sinh nhật)
 *   POINTS_EXPIRY_MONTHS  = 12       (tháng hết hạn)
 * </pre>
 */
@Entity
@Table(name = "loyalty_points_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoyaltyPointsConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Integer id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal configValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
