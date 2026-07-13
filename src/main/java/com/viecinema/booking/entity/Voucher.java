package com.viecinema.booking.entity;

import com.viecinema.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Ánh xạ bảng vouchers (mở rộng từ v10, chuẩn hoá thêm type v12).
 *
 * <p>Ba loại voucher:
 * <ul>
 *   <li>GIFT_CARD — Thẻ quà tặng dạng tiền mặt trừ dần (mệnh giá = current_balance)</li>
 *   <li>TICKET_DISCOUNT — Voucher giảm tiền vé từ đổi điểm loyalty</li>
 *   <li>COMBO_DISCOUNT — Voucher combo miễn phí từ đổi điểm loyalty</li>
 * </ul>
 */
@Entity
@Table(name = "vouchers", indexes = {
        @Index(name = "idx_code", columnList = "code"),
        @Index(name = "idx_owner", columnList = "owner_id"),
        @Index(name = "idx_status", columnList = "status"),
        @Index(name = "idx_type", columnList = "voucher_type")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "voucher_id")
    private Integer id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    // ── Loại voucher ──────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "voucher_type", nullable = false)
    private VoucherType voucherType;

    /**
     * Kiểu giảm giá của TICKET_DISCOUNT (null với GIFT_CARD, COMBO_DISCOUNT).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type")
    private DiscountType discountType;

    /**
     * Giá trị giảm: số tiền VND (AMOUNT) hoặc phần trăm (PERCENT).
     * Dùng cho TICKET_DISCOUNT.
     */
    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    // ── GIFT_CARD fields ──────────────────────────────────────────────────────

    /** Mệnh giá gốc (GIFT_CARD). */
    @Column(name = "original_value", precision = 10, scale = 2)
    private BigDecimal originalValue;

    /** Số dư còn lại có thể chi trả (GIFT_CARD). */
    @Column(name = "current_balance", precision = 10, scale = 2)
    private BigDecimal currentBalance;

    // ── COMBO_DISCOUNT fields ─────────────────────────────────────────────────

    /** Combo được đổi điểm lấy (COMBO_DISCOUNT). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "redeemed_combo_id")
    private Combo redeemedCombo;

    @Column(name = "redeemed_combo_quantity")
    private Integer redeemedComboQuantity = 1;

    // ── Sở hữu & phân phối ───────────────────────────────────────────────────

    /** User đã chi tiền/điểm để tạo voucher này. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchased_by")
    private User purchasedBy;

    /** User hiện đang sở hữu voucher. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    @Column(name = "recipient_name", length = 100)
    private String recipientName;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    // ── Vận hành ─────────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private VoucherStatus status = VoucherStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    private LocalDate expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Business logic ────────────────────────────────────────────────────────

    public boolean isActive() {
        return status == VoucherStatus.ACTIVE
                && (expiresAt == null || !LocalDate.now().isAfter(expiresAt));
    }

    // ── Enums ─────────────────────────────────────────────────────────────────

    public enum VoucherType {
        GIFT_CARD,        // Thẻ quà tặng tiền mặt
        TICKET_DISCOUNT,  // Giảm tiền vé từ đổi điểm
        COMBO_DISCOUNT    // Miễn phí combo từ đổi điểm
    }

    public enum DiscountType {
        AMOUNT,   // Giảm số tiền cố định (VND)
        PERCENT   // Giảm theo phần trăm
    }

    public enum VoucherStatus {
        PENDING,  // Chưa kích hoạt
        ACTIVE,   // Đang còn hiệu lực
        EXPIRED,  // Hết hạn
        LOCKED    // Bị khóa
    }
}
