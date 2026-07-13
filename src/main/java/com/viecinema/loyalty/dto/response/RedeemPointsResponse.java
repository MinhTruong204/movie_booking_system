package com.viecinema.loyalty.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.viecinema.booking.entity.Voucher.VoucherType;
import com.viecinema.loyalty.entity.PointRedemption.RedemptionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Response trả về sau khi đổi điểm thành công.
 */
@Data
@Builder
public class RedeemPointsResponse {

    private Integer redemptionId;

    private RedemptionType redemptionType;

    /** Số điểm đã tiêu. */
    private Integer pointsUsed;

    /** Số dư điểm còn lại sau khi đổi. */
    private Integer remainingPoints;

    // ── Thông tin voucher được tạo ────────────────────────────────────────────

    private Integer voucherId;

    private String voucherCode;

    private VoucherType voucherType;

    /** Giá trị quy đổi ra VND (cho TICKET_DISCOUNT). */
    private BigDecimal voucherValue;

    /** Tên combo (cho COMBO_DISCOUNT). */
    private String comboName;

    /** Số lượng combo (cho COMBO_DISCOUNT). */
    private Integer comboQuantity;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate voucherExpiresAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime redeemedAt;
}
