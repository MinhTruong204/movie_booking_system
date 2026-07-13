package com.viecinema.loyalty.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viecinema.booking.entity.Voucher.DiscountType;
import com.viecinema.booking.entity.Voucher.VoucherStatus;
import com.viecinema.booking.entity.Voucher.VoucherType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO hiển thị thông tin một voucher trong ví của user.
 * Dùng cho endpoint GET /api/loyalty/my-vouchers
 * và màn hình checkout để user chọn voucher.
 */
@Data
@Builder
public class VoucherDto {

    private Integer voucherId;

    private String code;

    private VoucherType voucherType;

    private VoucherStatus status;

    // ── GIFT_CARD ─────────────────────────────────────────────────────────────
    private BigDecimal originalValue;
    private BigDecimal currentBalance;

    // ── TICKET_DISCOUNT ───────────────────────────────────────────────────────
    private DiscountType discountType;
    private BigDecimal discountValue;

    // ── COMBO_DISCOUNT ────────────────────────────────────────────────────────
    private Integer comboId;
    private String comboName;
    private String comboImageUrl;
    private Integer comboQuantity;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
