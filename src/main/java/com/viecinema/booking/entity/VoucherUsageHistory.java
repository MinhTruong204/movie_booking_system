package com.viecinema.booking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ánh xạ bảng voucher_usage_history.
 * Ghi lại mỗi lần voucher được áp dụng vào một booking.
 */
@Entity
@Table(name = "voucher_usage_history", indexes = {
        @Index(name = "idx_voucher", columnList = "voucher_id"),
        @Index(name = "idx_booking", columnList = "booking_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoucherUsageHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usage_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Column(name = "booking_id", nullable = false)
    private Integer bookingId;

    @Column(name = "amount_used", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountUsed;

    @Column(name = "balance_before", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
