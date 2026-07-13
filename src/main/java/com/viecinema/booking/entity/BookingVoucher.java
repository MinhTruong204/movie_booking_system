package com.viecinema.booking.entity;

import com.viecinema.booking.entity.Voucher.VoucherType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Ánh xạ bảng booking_vouchers (v12).
 * Ghi lại voucher nào đã được áp dụng vào booking, cùng số tiền giảm thực tế.
 *
 * <p>Ràng buộc: mỗi cặp (booking_id, voucher_id) là duy nhất —
 * nghĩa là 1 voucher chỉ được dùng 1 lần cho 1 booking.
 */
@Entity
@Table(name = "booking_vouchers", indexes = {
        @Index(name = "idx_booking", columnList = "booking_id"),
        @Index(name = "idx_voucher", columnList = "voucher_id")
},
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_booking_voucher", columnNames = {"booking_id", "voucher_id"})
        })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingVoucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_voucher_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voucher_id", nullable = false)
    private Voucher voucher;

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", nullable = false)
    private VoucherType voucherType;

    /** Số tiền giảm thực tế được áp dụng từ voucher này. */
    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
