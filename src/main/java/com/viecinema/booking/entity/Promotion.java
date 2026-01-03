package com.viecinema.booking.entity;

import com.viecinema.common.entity.DeletableEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "promotions")
@SQLDelete(sql = "UPDATE promotions SET deleted_at = NOW() WHERE promo_id = ?")
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Promotion extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "promo_id")
    private Integer id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "min_order_value", precision = 10, scale = 2)
    private BigDecimal minOrderValue = BigDecimal.ZERO;

    @Column(name = "max_discount", precision = 10, scale = 2)
    private BigDecimal maxDiscount;

    @Column(name = "max_usage")
    private Integer maxUsage;

    @Column(name = "max_usage_per_user")
    private Integer maxUsagePerUser = 1;

    @Column(name = "current_usage")
    private Integer currentUsage = 0;

    // JSON array: [1, 5, 10] - movie IDs
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "applicable_movies", columnDefinition = "JSON")
    private List<Integer> applicableMovies;

    // Set: Mon, Tue, Wed...
    @ElementCollection(targetClass = DayOfWeek.class)
    @CollectionTable(name = "promotion_applicable_days",
            joinColumns = @JoinColumn(name = "promo_id"))
    @Column(name = "day")
    @Enumerated(EnumType.STRING)
    private Set<DayOfWeek> applicableDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // Business methods
    public boolean isValid() {
        if (!isActive) return false;

        LocalDateTime now = LocalDateTime.now();
        if (startDate != null && now.isBefore(startDate)) return false;
        if (endDate != null && now.isAfter(endDate)) return false;

        return maxUsage == null || currentUsage < maxUsage;
    }

    public boolean isApplicableForMovie(Integer movieId) {
        return applicableMovies == null || applicableMovies.isEmpty()
                || applicableMovies.contains(movieId);
    }

    public boolean isApplicableForDay(java.time.DayOfWeek dayOfWeek) {
        if (applicableDays == null || applicableDays.isEmpty()) return true;

        DayOfWeek day = DayOfWeek.fromJavaDayOfWeek(dayOfWeek);
        return applicableDays.contains(day);
    }

    public enum DiscountType {
        PERCENT, AMOUNT
    }

    public enum DayOfWeek {
        Mon, Tue, Wed, Thu, Fri, Sat, Sun;

        public static DayOfWeek fromJavaDayOfWeek(java.time.DayOfWeek javaDay) {
            return switch (javaDay) {
                case MONDAY -> Mon;
                case TUESDAY -> Tue;
                case WEDNESDAY -> Wed;
                case THURSDAY -> Thu;
                case FRIDAY -> Fri;
                case SATURDAY -> Sat;
                case SUNDAY -> Sun;
            };
        }
    }

}