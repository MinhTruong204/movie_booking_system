package com.viecinema.booking.service;

import com.viecinema.auth.entity.User;
import com.viecinema.booking.entity.*;
import com.viecinema.booking.entity.Voucher.VoucherStatus;
import com.viecinema.booking.entity.Voucher.VoucherType;
import com.viecinema.booking.entity.Voucher.DiscountType;
import com.viecinema.booking.repository.*;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.exception.SpecificBusinessException;
import com.viecinema.loyalty.dto.response.VoucherDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service xử lý toàn bộ nghiệp vụ Voucher.
 *
 * <p>Các chức năng chính:
 * <ol>
 *   <li>{@link #getMyVouchers} — Lấy danh sách voucher active của user (cho ví voucher)</li>
 *   <li>{@link #createVoucherFromRedemption} — Tạo voucher mới khi user đổi điểm</li>
 *   <li>{@link #calculateTicketVoucherDiscount} — Tính tiền giảm từ TICKET_DISCOUNT</li>
 *   <li>{@link #calculateComboVoucherDiscount} — Tính tiền giảm từ COMBO_DISCOUNT</li>
 *   <li>{@link #commitVoucherUsage} — Commit usage + cập nhật trạng thái voucher sau booking</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherUsageHistoryRepository voucherUsageHistoryRepository;
    private final BookingVoucherRepository bookingVoucherRepository;

    // =========================================================================
    // 1. QUERY — Ví voucher
    // =========================================================================

    /**
     * Lấy danh sách tất cả voucher active của user — dùng cho màn hình ví voucher và checkout.
     *
     * @param userId ID của user
     * @return Danh sách VoucherDto, sắp xếp theo ngày hết hạn gần nhất
     */
    @Transactional(readOnly = true)
    public List<VoucherDto> getMyVouchers(Integer userId) {
        List<Voucher> vouchers = voucherRepository.findActiveVouchersByOwner(userId, LocalDate.now());
        return vouchers.stream().map(this::toDto).collect(Collectors.toList());
    }

    /**
     * Lấy voucher active của user theo loại cụ thể.
     *
     * @param userId ID user
     * @param type   Loại voucher (TICKET_DISCOUNT hoặc COMBO_DISCOUNT)
     */
    @Transactional(readOnly = true)
    public List<VoucherDto> getMyVouchersByType(Integer userId, VoucherType type) {
        List<Voucher> vouchers = voucherRepository.findActiveVouchersByOwnerAndType(
                userId, type, LocalDate.now());
        return vouchers.stream().map(this::toDto).collect(Collectors.toList());
    }

    // =========================================================================
    // 2. TẠO VOUCHER TỪ ĐỔI ĐIỂM
    // =========================================================================

    /**
     * Tạo voucher TICKET_DISCOUNT từ đổi điểm.
     * Gọi bởi {@link com.viecinema.loyalty.service.LoyaltyPointsService#redeemPointsForVoucher}.
     *
     * @param owner       User sở hữu voucher
     * @param voucherValue Giá trị voucher (VND) = pointsToUse * REDEEM_RATE_PER_POINT
     * @param expiresAt   Ngày hết hạn
     * @return Voucher vừa được tạo
     */
    @Transactional
    public Voucher createTicketDiscountVoucher(User owner, BigDecimal voucherValue, LocalDate expiresAt) {
        Voucher voucher = Voucher.builder()
                .code(generateVoucherCode("TKT"))
                .voucherType(VoucherType.TICKET_DISCOUNT)
                .discountType(DiscountType.AMOUNT)
                .discountValue(voucherValue)
                // current_balance và original_value là NOT NULL trong DB;
                // với TICKET_DISCOUNT chúng không có ý nghĩa nên đặt ZERO.
                .originalValue(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .redeemedComboQuantity(0)
                .purchasedBy(owner)
                .owner(owner)
                .status(VoucherStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();

        voucher = voucherRepository.save(voucher);
        log.info("[Voucher] Tạo TICKET_DISCOUNT voucher {} ({} VND) cho user {}",
                voucher.getCode(), voucherValue, owner.getId());
        return voucher;
    }

    /**
     * Tạo voucher COMBO_DISCOUNT từ đổi điểm.
     * Gọi bởi {@link com.viecinema.loyalty.service.LoyaltyPointsService#redeemPointsForCombo}.
     *
     * @param owner    User sở hữu voucher
     * @param combo    Combo được đổi
     * @param quantity Số lượng combo
     * @param expiresAt Ngày hết hạn
     * @return Voucher vừa được tạo
     */
    @Transactional
    public Voucher createComboDiscountVoucher(User owner, Combo combo, int quantity, LocalDate expiresAt) {
        Voucher voucher = Voucher.builder()
                .code(generateVoucherCode("CMB"))
                .voucherType(VoucherType.COMBO_DISCOUNT)
                .redeemedCombo(combo)
                .redeemedComboQuantity(quantity)
                // current_balance và original_value là NOT NULL trong DB;
                // với COMBO_DISCOUNT chúng không có ý nghĩa nên đặt ZERO.
                .originalValue(BigDecimal.ZERO)
                .currentBalance(BigDecimal.ZERO)
                .purchasedBy(owner)
                .owner(owner)
                .status(VoucherStatus.ACTIVE)
                .expiresAt(expiresAt)
                .build();

        voucher = voucherRepository.save(voucher);
        log.info("[Voucher] Tạo COMBO_DISCOUNT voucher {} (combo={}, qty={}) cho user {}",
                voucher.getCode(), combo.getName(), quantity, owner.getId());
        return voucher;
    }

    // =========================================================================
    // 3. TÍNH DISCOUNT KHI ĐẶT VÉ
    // =========================================================================

    /**
     * Validate voucher TICKET_DISCOUNT và tính số tiền giảm.
     *
     * @param voucherId   ID voucher
     * @param userId      User đang đặt vé (phải là chủ sở hữu)
     * @param ticketsSubtotal Tổng tiền vé trước giảm giá
     * @return Số tiền được giảm (≥ 0)
     * @throws SpecificBusinessException nếu voucher không hợp lệ
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTicketVoucherDiscount(Integer voucherId, Integer userId,
                                                      BigDecimal ticketsSubtotal) {
        if (voucherId == null) return BigDecimal.ZERO;

        Voucher voucher = getAndValidateVoucher(voucherId, userId, VoucherType.TICKET_DISCOUNT);

        return switch (voucher.getDiscountType()) {
            case AMOUNT -> voucher.getDiscountValue().min(ticketsSubtotal);
            case PERCENT -> ticketsSubtotal
                    .multiply(voucher.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                    .min(ticketsSubtotal);
        };
    }

    /**
     * Validate voucher COMBO_DISCOUNT và tính số tiền giảm trên combos.
     * Logic: Giảm giá bằng giá trị combo * quantity đã đổi, tối đa không quá combosSubtotal.
     *
     * @param voucherId      ID voucher
     * @param userId         User đang đặt vé
     * @param combosSubtotal Tổng tiền combo trước giảm giá
     * @return Số tiền được giảm (≥ 0)
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateComboVoucherDiscount(Integer voucherId, Integer userId,
                                                     BigDecimal combosSubtotal) {
        if (voucherId == null) return BigDecimal.ZERO;

        Voucher voucher = getAndValidateVoucher(voucherId, userId, VoucherType.COMBO_DISCOUNT);

        Combo combo = voucher.getRedeemedCombo();
        if (combo == null) return BigDecimal.ZERO;

        BigDecimal comboValue = combo.getPrice()
                .multiply(BigDecimal.valueOf(voucher.getRedeemedComboQuantity()));

        // Giảm tối đa bằng giá trị combo hoặc combosSubtotal (lấy min)
        return comboValue.min(combosSubtotal);
    }

    // =========================================================================
    // 4. COMMIT USAGE SAU KHI ĐẶT VÉ THÀNH CÔNG
    // =========================================================================

    /**
     * Ghi nhận việc áp dụng voucher vào booking.
     * Gọi sau khi booking được lưu thành công.
     *
     * @param booking        Booking đã được tạo
     * @param voucherId      ID voucher cần commit
     * @param discountAmount Số tiền đã giảm thực tế
     * @param voucherType    Loại voucher
     */
    @Transactional
    public void commitVoucherUsage(Booking booking, Integer voucherId,
                                    BigDecimal discountAmount, VoucherType voucherType) {
        if (voucherId == null || discountAmount.compareTo(BigDecimal.ZERO) == 0) return;

        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher"));

        // Idempotency — tránh duplicate
        if (bookingVoucherRepository.existsByBooking_IdAndVoucher_Id(booking.getId(), voucherId)) {
            log.warn("[Voucher] Booking {} đã có voucher {} được commit, bỏ qua.", booking.getId(), voucherId);
            return;
        }

        // 1. Ghi vào booking_vouchers
        BookingVoucher bookingVoucher = BookingVoucher.builder()
                .booking(booking)
                .voucher(voucher)
                .voucherType(voucherType)
                .discountAmount(discountAmount)
                .build();
        bookingVoucherRepository.save(bookingVoucher);

        // 2. Ghi vào voucher_usage_history (chỉ áp dụng cho GIFT_CARD)
        if (voucherType == VoucherType.GIFT_CARD) {
            BigDecimal balanceBefore = voucher.getCurrentBalance();
            BigDecimal balanceAfter = balanceBefore.subtract(discountAmount).max(BigDecimal.ZERO);

            VoucherUsageHistory history = VoucherUsageHistory.builder()
                    .voucher(voucher)
                    .bookingId(booking.getId())
                    .amountUsed(discountAmount)
                    .balanceBefore(balanceBefore)
                    .balanceAfter(balanceAfter)
                    .build();
            voucherUsageHistoryRepository.save(history);

            voucher.setCurrentBalance(balanceAfter);
        }

        // 3. Đánh dấu voucher đã dùng (TICKET_DISCOUNT và COMBO_DISCOUNT: dùng 1 lần)
        if (voucherType != VoucherType.GIFT_CARD) {
            voucher.setStatus(VoucherStatus.LOCKED);
        }

        voucherRepository.save(voucher);

        log.info("[Voucher] Commit voucher {} ({}) vào booking {} — giảm {} VND",
                voucher.getCode(), voucherType, booking.getId(), discountAmount);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Validate voucher: tồn tại, thuộc user, đúng loại, còn hiệu lực.
     */
    private Voucher getAndValidateVoucher(Integer voucherId, Integer userId, VoucherType expectedType) {
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new ResourceNotFoundException("Voucher không tồn tại: " + voucherId));

        if (voucher.getOwner() == null || !voucher.getOwner().getId().equals(userId)) {
            throw new SpecificBusinessException("Voucher không thuộc về tài khoản của bạn.");
        }

        if (voucher.getVoucherType() != expectedType) {
            throw new SpecificBusinessException(
                    "Voucher " + voucher.getCode() + " không phải loại " + expectedType.name());
        }

        if (!voucher.isActive()) {
            throw new SpecificBusinessException(
                    "Voucher " + voucher.getCode() + " đã hết hạn hoặc không còn hiệu lực.");
        }

        return voucher;
    }

    private String generateVoucherCode(String prefix) {
        String random = Long.toHexString(System.nanoTime()).toUpperCase();
        return prefix + random.substring(Math.max(0, random.length() - 8));
    }

    private VoucherDto toDto(Voucher v) {
        VoucherDto.VoucherDtoBuilder builder = VoucherDto.builder()
                .voucherId(v.getId())
                .code(v.getCode())
                .voucherType(v.getVoucherType())
                .status(v.getStatus())
                .discountType(v.getDiscountType())
                .discountValue(v.getDiscountValue())
                .originalValue(v.getOriginalValue())
                .currentBalance(v.getCurrentBalance())
                .expiresAt(v.getExpiresAt())
                .createdAt(v.getCreatedAt());

        if (v.getRedeemedCombo() != null) {
            builder.comboId(v.getRedeemedCombo().getId())
                    .comboName(v.getRedeemedCombo().getName())
                    .comboImageUrl(v.getRedeemedCombo().getImageUrl())
                    .comboQuantity(v.getRedeemedComboQuantity());
        }

        return builder.build();
    }
}
