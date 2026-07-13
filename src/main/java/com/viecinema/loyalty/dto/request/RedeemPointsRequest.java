package com.viecinema.loyalty.dto.request;

import com.viecinema.loyalty.entity.PointRedemption.RedemptionType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request đổi điểm lấy voucher hoặc combo.
 *
 * <ul>
 *   <li>Nếu {@code redemptionType = VOUCHER}: bắt buộc có {@code pointsToUse} ≥ min config</li>
 *   <li>Nếu {@code redemptionType = COMBO}: bắt buộc có {@code comboId}; điểm tính tự động</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedeemPointsRequest {

    @NotNull(message = "Vui lòng chọn loại đổi điểm: VOUCHER hoặc COMBO")
    private RedemptionType redemptionType;

    /**
     * Số điểm muốn dùng (bắt buộc nếu type = VOUCHER).
     * Giá trị voucher = pointsToUse * REDEEM_RATE_PER_POINT (VND).
     */
    @Min(value = 1, message = "Số điểm phải lớn hơn 0")
    private Integer pointsToUse;

    /** ID combo muốn đổi (bắt buộc nếu type = COMBO). */
    private Integer comboId;

    /** Số lượng combo muốn đổi (mặc định 1, chỉ dùng khi type = COMBO). */
    @Min(value = 1, message = "Số lượng phải ít nhất là 1")
    @Builder.Default
    private Integer quantity = 1;
}
