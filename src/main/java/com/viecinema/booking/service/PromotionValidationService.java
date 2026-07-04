package com.viecinema.booking.service;

import com.viecinema.auth.entity.User;
import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.BookingPromotion;
import com.viecinema.booking.entity.Promotion;
import com.viecinema.booking.entity.UserPromotionUsage;
import com.viecinema.booking.repository.BookingPromotionRepository;
import com.viecinema.booking.repository.PromotionRepository;
import com.viecinema.booking.repository.UserPromotionUsageRepository;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.exception.SpecificBusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Service xử lý toàn bộ luồng mã khuyến mãi theo 4 bước:
 * <ol>
 *   <li>Validate thông tin Promotion (tồn tại, trạng thái, thời gian, giới hạn)</li>
 *   <li>Validate điều kiện Đơn hàng (giá trị tối thiểu, phim, ngày chiếu)</li>
 *   <li>Tính toán giảm giá (PERCENT / AMOUNT với giới hạn maxDiscount)</li>
 *   <li>Commit dữ liệu vào booking_promotions, promotions, user_promotion_usage</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromotionValidationService {

    private final PromotionRepository promotionRepository;
    private final UserPromotionUsageRepository userPromotionUsageRepository;
    private final BookingPromotionRepository bookingPromotionRepository;

    // =========================================================================
    // PUBLIC API
    // =========================================================================

    /**
     * Validate mã KM và tính số tiền giảm.
     * Được gọi từ {@link BookingCalculationService} khi preview giá,
     * và từ {@link BookingService} khi tạo booking.
     *
     * @param code       mã khuyến mãi người dùng nhập
     * @param userId     id của user (để kiểm tra giới hạn cá nhân)
     * @param subtotal   tổng tiền đơn hàng trước giảm
     * @param movieId    id phim đang đặt (kiểm tra applicable_movies)
     * @param showtimeStart thời điểm bắt đầu suất chiếu (kiểm tra applicable_days)
     * @return số tiền được giảm (>= 0)
     * @throws ResourceNotFoundException   nếu code không tồn tại
     * @throws SpecificBusinessException   nếu vi phạm bất kỳ quy tắc nào
     */
    @Transactional(readOnly = true)
    public BigDecimal validateAndCalculate(
            String code,
            Integer userId,
            BigDecimal subtotal,
            Integer movieId,
            LocalDateTime showtimeStart) {

        // ── Bước 1: Validate thông tin Promotion ──────────────────────────────
        Promotion promo = validatePromotion(code, userId);

        // ── Bước 2: Validate điều kiện Đơn hàng ──────────────────────────────
        validateOrderConstraints(promo, subtotal, movieId, showtimeStart);

        // ── Bước 3: Tính toán giảm giá ────────────────────────────────────────
        return calculateDiscount(promo, subtotal);
    }

    /**
     * Bước 4: Commit dữ liệu sau khi thanh toán thành công.
     * Lưu {@code booking_promotions}, tăng {@code promotions.current_usage},
     * cập nhật {@code user_promotion_usage}.
     *
     * @param booking        booking đã được xác nhận thanh toán
     * @param promoCode      mã khuyến mãi đã áp dụng
     * @param discountAmount số tiền giảm thực tế
     */
    @Transactional
    public void commitPromotion(Booking booking, String promoCode, BigDecimal discountAmount) {
        // Dùng lock để tránh race condition khi nhiều user cùng commit
        Promotion promo = promotionRepository.findByCodeWithLock(promoCode)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion"));

        User user = booking.getUser();

        // 4.1 Lưu vào booking_promotions
        BookingPromotion bookingPromotion = BookingPromotion.builder()
                .booking(booking)
                .promo(promo)
                .discountAmount(discountAmount)
                .build();
        bookingPromotionRepository.save(bookingPromotion);

        // 4.2 Tăng current_usage của promotion
        promo.setCurrentUsage(promo.getCurrentUsage() + 1);
        promotionRepository.save(promo);

        // 4.3 Cập nhật user_promotion_usage
        UserPromotionUsage usage = userPromotionUsageRepository
                .findByUserIdAndPromoId(user.getId(), promo.getId())
                .orElseGet(() -> {
                    UserPromotionUsage newUsage = new UserPromotionUsage();
                    newUsage.setUser(user);
                    newUsage.setPromo(promo);
                    newUsage.setUsageCount(0);
                    newUsage.setCreatedAt(Instant.now());
                    return newUsage;
                });

        usage.setUsageCount(usage.getUsageCount() + 1);
        usage.setLastUsedAt(Instant.now());
        usage.setUpdatedAt(Instant.now());
        userPromotionUsageRepository.save(usage);

        log.info("Committed promotion '{}' for booking {} — discount: {}",
                promoCode, booking.getBookingCode(), discountAmount);
    }

    // =========================================================================
    // BƯỚC 1 — VALIDATE PROMOTION
    // =========================================================================

    /**
     * Chuỗi kiểm tra thác nước (fail fast): tồn tại → trạng thái → thời gian
     * → giới hạn hệ thống → giới hạn cá nhân.
     */
    private Promotion validatePromotion(String code, Integer userId) {

        // 1.1 Tồn tại
        Promotion promo = promotionRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion code '" + code + "' không tồn tại"));

        // 1.2 Trạng thái active
        if (!Boolean.TRUE.equals(promo.getIsActive())) {
            throw new SpecificBusinessException("Mã khuyến mãi '" + code + "' đã bị vô hiệu hóa.");
        }

        // 1.3 Thời gian: chưa bắt đầu
        LocalDateTime now = LocalDateTime.now();
        if (promo.getStartDate() != null && now.isBefore(promo.getStartDate())) {
            throw new SpecificBusinessException(
                    "Mã khuyến mãi '" + code + "' chưa có hiệu lực. Hiệu lực từ: " + promo.getStartDate());
        }

        // 1.4 Thời gian: đã hết hạn
        if (promo.getEndDate() != null && now.isAfter(promo.getEndDate())) {
            throw new SpecificBusinessException("Mã khuyến mãi '" + code + "' đã hết hạn.");
        }

        // 1.5 Giới hạn hệ thống (tổng lượt dùng)
        if (promo.getMaxUsage() != null && promo.getCurrentUsage() >= promo.getMaxUsage()) {
            throw new SpecificBusinessException("Mã khuyến mãi '" + code + "' đã hết lượt sử dụng.");
        }

        // 1.6 Giới hạn cá nhân (per-user)
        if (promo.getMaxUsagePerUser() != null) {
            int userUsageCount = userPromotionUsageRepository
                    .findByUserIdAndPromoId(userId, promo.getId())
                    .map(UserPromotionUsage::getUsageCount)
                    .orElse(0);

            if (userUsageCount >= promo.getMaxUsagePerUser()) {
                throw new SpecificBusinessException(
                        "Bạn đã sử dụng hết " + promo.getMaxUsagePerUser()
                                + " lượt cho mã khuyến mãi '" + code + "'.");
            }
        }

        return promo;
    }

    // =========================================================================
    // BƯỚC 2 — VALIDATE ĐIỀU KIỆN ĐƠN HÀNG
    // =========================================================================

    private void validateOrderConstraints(
            Promotion promo,
            BigDecimal subtotal,
            Integer movieId,
            LocalDateTime showtimeStart) {

        // 2.1 Giá trị đơn hàng tối thiểu
        if (promo.getMinOrderValue() != null
                && subtotal.compareTo(promo.getMinOrderValue()) < 0) {
            throw new SpecificBusinessException(
                    "Giá trị đơn hàng tối thiểu để dùng mã này là "
                            + promo.getMinOrderValue().toPlainString() + " VNĐ. "
                            + "Đơn hàng hiện tại: " + subtotal.toPlainString() + " VNĐ.");
        }

        // 2.2 Phim áp dụng
        if (!promo.isApplicableForMovie(movieId)) {
            throw new SpecificBusinessException(
                    "Mã khuyến mãi không áp dụng cho phim này.");
        }

        // 2.3 Ngày chiếu áp dụng
        if (showtimeStart != null && !promo.isApplicableForDay(showtimeStart.getDayOfWeek())) {
            throw new SpecificBusinessException(
                    "Mã khuyến mãi không áp dụng vào ngày chiếu này ("
                            + showtimeStart.getDayOfWeek() + ").");
        }
    }

    // =========================================================================
    // BƯỚC 3 — TÍNH TOÁN GIẢM GIÁ
    // =========================================================================

    private BigDecimal calculateDiscount(Promotion promo, BigDecimal subtotal) {
        BigDecimal discount;

        switch (promo.getDiscountType()) {
            case PERCENT -> {
                // discount = subtotal * discountValue / 100
                discount = subtotal
                        .multiply(promo.getDiscountValue())
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

                // Áp dụng giới hạn tối đa (maxDiscount)
                if (promo.getMaxDiscount() != null
                        && discount.compareTo(promo.getMaxDiscount()) > 0) {
                    discount = promo.getMaxDiscount();
                }
            }
            case AMOUNT -> {
                discount = promo.getDiscountValue();

                // Giảm không được vượt quá subtotal (tránh finalAmount âm)
                if (discount.compareTo(subtotal) > 0) {
                    discount = subtotal;
                }
            }
            default -> throw new SpecificBusinessException(
                    "Loại giảm giá không hợp lệ: " + promo.getDiscountType());
        }

        log.debug("Promotion '{}' ({}) — subtotal: {}, discount: {}",
                promo.getCode(), promo.getDiscountType(), subtotal, discount);

        return discount;
    }
}
