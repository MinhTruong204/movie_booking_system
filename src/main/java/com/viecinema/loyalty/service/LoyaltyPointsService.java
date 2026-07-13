package com.viecinema.loyalty.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.MembershipTierRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.Combo;
import com.viecinema.booking.entity.Voucher;
import com.viecinema.booking.repository.ComboRepository;
import com.viecinema.booking.service.VoucherService;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.exception.SpecificBusinessException;
import com.viecinema.loyalty.entity.LoyaltyPointsConfig;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory.PointsType;
import com.viecinema.loyalty.entity.PointRedemption;
import com.viecinema.loyalty.entity.PointRedemption.RedemptionType;
import com.viecinema.loyalty.repository.LoyaltyPointsConfigRepository;
import com.viecinema.loyalty.repository.LoyaltyPointsHistoryRepository;
import com.viecinema.loyalty.repository.PointRedemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Service trung tâm xử lý tất cả nghiệp vụ tích điểm.
 *
 * <p>3 loại tích điểm được hỗ trợ:
 * <ol>
 *   <li>{@link #awardTransactionPoints} — Cộng điểm EARN khi booking chuyển sang PAID</li>
 *   <li>{@link #awardReviewBonus} — Cộng điểm BONUS khi review có is_verified_booking = TRUE</li>
 *   <li>{@link #awardBirthdayBonus} — Cộng điểm BONUS sinh nhật (gọi từ Scheduler)</li>
 * </ol>
 *
 * <p>Tất cả phương thức đều có cơ chế <strong>idempotency</strong> — gọi nhiều lần vẫn an toàn.
 *
 * <p><strong>Lưu ý khách vãng lai (GUEST)</strong>: Điểm vẫn được ghi vào DB nhưng
 * user role=GUEST không thể sử dụng điểm (kiểm soát ở tầng REDEEM).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoyaltyPointsService {

    // Config keys
    public static final String KEY_EARN_RATE = "EARN_RATE_PER_VND";
    public static final String KEY_REVIEW_BONUS = "REVIEW_BONUS_POINTS";
    public static final String KEY_BIRTHDAY_BONUS = "BIRTHDAY_BONUS_POINTS";
    public static final String KEY_EXPIRY_MONTHS = "POINTS_EXPIRY_MONTHS";
    public static final String KEY_REDEEM_RATE = "REDEEM_RATE_PER_POINT";
    public static final String KEY_REDEEM_MIN = "REDEEM_MIN_POINTS";
    public static final String KEY_VOUCHER_EXPIRY_DAYS = "VOUCHER_EXPIRY_DAYS";

    // Fallback defaults (nếu DB config chưa seed)
    private static final BigDecimal DEFAULT_EARN_RATE = new BigDecimal("0.0001");
    private static final int DEFAULT_REVIEW_BONUS = 20;
    private static final int DEFAULT_BIRTHDAY_BONUS = 100;
    private static final int DEFAULT_EXPIRY_MONTHS = 12;
    private static final BigDecimal DEFAULT_REDEEM_RATE = new BigDecimal("100"); // 1 điểm = 100 VND
    private static final int DEFAULT_REDEEM_MIN = 100;
    private static final int DEFAULT_VOUCHER_EXPIRY_DAYS = 90;

    private final LoyaltyPointsHistoryRepository historyRepository;
    private final LoyaltyPointsConfigRepository configRepository;
    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final PointRedemptionRepository redemptionRepository;
    private final ComboRepository comboRepository;
    private final VoucherService voucherService;

    // ============================================================
    // 1. TRANSACTION-BASED EARNING
    // ============================================================

    /**
     * Cộng điểm EARN khi booking chuyển sang PAID.
     * Công thức: points = floor(finalAmount * EARN_RATE_PER_VND)
     *
     * <p>Idempotent: kiểm tra {@code existsByBookingIdAndPointsType(EARN)} trước khi insert.
     * Áp dụng cho cả GUEST (điểm ghi DB nhưng không dùng được).
     *
     * @param booking Booking đã chuyển sang trạng thái PAID
     */
    @Transactional
    public void awardTransactionPoints(Booking booking) {
        Integer bookingId = booking.getId();
        User user = booking.getUser();

        // Idempotency check — tránh double credit
        if (historyRepository.existsByBookingIdAndPointsType(bookingId, PointsType.EARN)) {
            log.warn("[Loyalty] Booking {} đã được tích điểm EARN, bỏ qua.", bookingId);
            return;
        }

        // Cập nhật tổng chi tiêu (total_spent) của User
        BigDecimal finalAmount = booking.getFinalAmount();
        BigDecimal oldTotalSpent = user.getTotalSpent() != null ? user.getTotalSpent() : BigDecimal.ZERO;
        user.setTotalSpent(oldTotalSpent.add(finalAmount));
        userRepository.save(user);
        log.info("[Loyalty] Cập nhật tổng chi tiêu cho user {}: {} -> {}",
                user.getId(), oldTotalSpent, user.getTotalSpent());

        BigDecimal earnRate = getConfigValue(KEY_EARN_RATE, DEFAULT_EARN_RATE);
        int points = booking.getFinalAmount()
                .multiply(earnRate)
                .intValue(); // floor tự động

        if (points <= 0) {
            log.info("[Loyalty] Booking {} có final_amount quá nhỏ, không đủ điểm EARN.", bookingId);
            return;
        }

        int expiryMonths = getConfigInt(KEY_EXPIRY_MONTHS, DEFAULT_EXPIRY_MONTHS);
        LocalDate expiresAt = expiryMonths > 0
                ? LocalDate.now().plusMonths(expiryMonths)
                : null;

        String desc = String.format("Tích điểm từ đơn hàng #%s (%.0f VND)",
                booking.getBookingCode(), booking.getFinalAmount());

        creditPoints(user, points, PointsType.EARN, desc, bookingId, null, expiresAt);

        log.info("[Loyalty] Booking {} → EARN {} điểm cho user {} ({})",
                bookingId, points, user.getId(), user.getEmail());
    }

    // ============================================================
    // 2. ENGAGEMENT BONUS — REVIEW
    // ============================================================

    /**
     * Cộng điểm BONUS khi user để lại review có is_verified_booking = TRUE.
     *
     * <p>Idempotent: kiểm tra {@code existsByReviewId(reviewId)} trước khi insert.
     * Áp dụng cho cả GUEST.
     *
     * @param userId   ID của user để lại review
     * @param reviewId ID của review vừa được tạo/xác thực
     */
    @Transactional
    public void awardReviewBonus(Integer userId, Integer reviewId) {
        // Idempotency check
        if (historyRepository.existsByReviewId(reviewId)) {
            log.warn("[Loyalty] Review {} đã được thưởng điểm BONUS, bỏ qua.", reviewId);
            return;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("[Loyalty] Không tìm thấy user {} khi thưởng điểm review.", userId);
            return;
        }

        int points = getConfigInt(KEY_REVIEW_BONUS, DEFAULT_REVIEW_BONUS);
        String desc = String.format("Thưởng điểm do để lại đánh giá phim (review #%d)", reviewId);

        creditPoints(user, points, PointsType.BONUS, desc, null, reviewId, null);

        log.info("[Loyalty] Review {} → BONUS {} điểm cho user {} ({})",
                reviewId, points, userId, user.getEmail());
    }

    // ============================================================
    // 3. EVENT BONUS — BIRTHDAY
    // ============================================================

    /**
     * Cộng điểm BONUS sinh nhật cho user.
     * Gọi từ {@link com.viecinema.loyalty.scheduler.BirthdayBonusScheduler} mỗi ngày lúc 8h sáng.
     *
     * <p>Idempotent: kiểm tra đã tặng năm nay chưa dựa vào created_at trong năm hiện tại.
     * Áp dụng cho cả GUEST.
     *
     * @param user User có sinh nhật hôm nay
     */
    @Transactional
    public void awardBirthdayBonus(User user) {
        // Idempotency: kiểm tra đã tặng trong năm nay chưa
        LocalDateTime startOfYear = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime endOfYear = LocalDate.now().withDayOfYear(1).plusYears(1).atStartOfDay();

        boolean alreadyAwarded = historyRepository
                .existsByUser_IdAndPointsTypeAndDescriptionContainingAndCreatedAtBetween(
                        user.getId(),
                        PointsType.BONUS,
                        "sinh nhật",
                        startOfYear,
                        endOfYear
                );

        if (alreadyAwarded) {
            log.warn("[Loyalty] User {} đã nhận điểm sinh nhật năm nay.", user.getId());
            return;
        }

        int points = getConfigInt(KEY_BIRTHDAY_BONUS, DEFAULT_BIRTHDAY_BONUS);
        String desc = String.format("Chúc mừng sinh nhật! Tặng %d điểm từ VieCinema 🎂", points);

        creditPoints(user, points, PointsType.BONUS, desc, null, null, null);

        log.info("[Loyalty] Birthday BONUS {} điểm → user {} ({})",
                points, user.getId(), user.getEmail());
    }

    // ============================================================
    // PRIVATE HELPERS
    // ============================================================

    /**
     * Cộng điểm vào tài khoản user và ghi lịch sử.
     * Tất cả thao tác trong cùng 1 transaction để đảm bảo tính nhất quán.
     */
    private void creditPoints(
            User user,
            int points,
            PointsType type,
            String description,
            Integer bookingId,
            Integer reviewId,
            LocalDate expiresAt
    ) {
        int oldBalance = user.getLoyaltyPoints() == null ? 0 : user.getLoyaltyPoints();
        int newBalance = oldBalance + points;

        // Cập nhật số điểm user
        user.setLoyaltyPoints(newBalance);
        userRepository.save(user);

        // Kiểm tra và nâng hạng membership nếu đủ điểm
        upgradeUserTierIfEligible(user, newBalance);

        // Ghi lịch sử
        LoyaltyPointsHistory history = LoyaltyPointsHistory.builder()
                .user(user)
                .bookingId(bookingId)
                .reviewId(reviewId)
                .pointsChange(points)
                .pointsType(type)
                .description(description)
                .oldBalance(oldBalance)
                .newBalance(newBalance)
                .expiresAt(expiresAt)
                .build();

        historyRepository.save(history);
    }

    /**
     * Kiểm tra và tự động nâng hạng membership dựa trên tổng điểm tích lũy.
     * Lấy hạng cao nhất mà user đủ điều kiện.
     */
    private void upgradeUserTierIfEligible(User user, int currentPoints) {
        membershipTierRepository.findTopEligibleTier(currentPoints).ifPresent(tier -> {
            Integer currentTierId = user.getMembershipTier() != null
                    ? user.getMembershipTier().getId()
                    : null;
            if (!tier.getId().equals(currentTierId)) {
                user.setMembershipTier(tier);
                userRepository.save(user);
                log.info("[Loyalty] User {} nâng hạng lên {} ({} điểm)",
                        user.getId(), tier.getName(), currentPoints);
            }
        });
    }

    /**
     * Đọc config dạng BigDecimal từ DB, fallback về giá trị mặc định nếu thiếu.
     */
    private BigDecimal getConfigValue(String key, BigDecimal defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(LoyaltyPointsConfig::getConfigValue)
                .orElse(defaultValue);
    }

    /**
     * Đọc config dạng int từ DB, fallback về giá trị mặc định nếu thiếu.
     */
    private int getConfigInt(String key, int defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(c -> c.getConfigValue().intValue())
                .orElse(defaultValue);
    }

    /**
     * Trả về số điểm BONUS cho review (dùng để include vào response API).
     */
    public int getReviewBonusPoints() {
        return getConfigInt(KEY_REVIEW_BONUS, DEFAULT_REVIEW_BONUS);
    }

    // ============================================================
    // 4. REDEEM POINTS — Đổi điểm lấy Voucher
    // ============================================================

    /**
     * Trừ điểm và tạo voucher TICKET_DISCOUNT.
     * Công thức: voucherValue = pointsToUse * REDEEM_RATE_PER_POINT (VND).
     *
     * @param userId      ID user muốn đổi điểm
     * @param pointsToUse Số điểm muốn tiêu
     * @return PointRedemption vừa được tạo
     */
    @Transactional
    public PointRedemption redeemPointsForVoucher(Integer userId, int pointsToUse) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        validateRedeemRequest(user, pointsToUse);

        BigDecimal rate = getConfigValue(KEY_REDEEM_RATE, DEFAULT_REDEEM_RATE);
        BigDecimal voucherValue = BigDecimal.valueOf(pointsToUse).multiply(rate);
        int expiryDays = getConfigInt(KEY_VOUCHER_EXPIRY_DAYS, DEFAULT_VOUCHER_EXPIRY_DAYS);
        LocalDate expiresAt = LocalDate.now().plusDays(expiryDays);

        // Tạo voucher
        Voucher voucher = voucherService.createTicketDiscountVoucher(user, voucherValue, expiresAt);

        // Trừ điểm
        String desc = String.format("Đổi %d điểm lấy voucher giảm %,.0f VND (mã %s)",
                pointsToUse, voucherValue, voucher.getCode());
        debitPoints(user, pointsToUse, PointsType.REDEEM, desc, null);

        // Ghi lịch sử đổi điểm
        PointRedemption redemption = PointRedemption.builder()
                .user(user)
                .pointsUsed(pointsToUse)
                .redemptionType(RedemptionType.VOUCHER)
                .voucherId(voucher.getId())
                .description(desc)
                .build();
        redemption = redemptionRepository.save(redemption);

        log.info("[Loyalty] User {} đổi {} điểm lấy voucher {} ({} VND)",
                userId, pointsToUse, voucher.getCode(), voucherValue);
        return redemption;
    }

    /**
     * Trừ điểm và tạo voucher COMBO_DISCOUNT.
     * Số điểm cần = ceil(combo.price * quantity / REDEEM_RATE_PER_POINT).
     *
     * @param userId   ID user
     * @param comboId  ID combo muốn đổi
     * @param quantity Số lượng combo
     * @return PointRedemption vừa được tạo
     */
    @Transactional
    public PointRedemption redeemPointsForCombo(Integer userId, Integer comboId, int quantity) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        Combo combo = comboRepository.findById(comboId)
                .orElseThrow(() -> new ResourceNotFoundException("Combo"));

        if (!Boolean.TRUE.equals(combo.getIsActive())) {
            throw new SpecificBusinessException("Combo " + combo.getName() + " hiện không còn bán.");
        }

        BigDecimal rate = getConfigValue(KEY_REDEEM_RATE, DEFAULT_REDEEM_RATE);
        BigDecimal totalComboValue = combo.getPrice().multiply(BigDecimal.valueOf(quantity));
        int pointsNeeded = totalComboValue.divide(rate, 0, java.math.RoundingMode.CEILING).intValue();

        validateRedeemRequest(user, pointsNeeded);

        int expiryDays = getConfigInt(KEY_VOUCHER_EXPIRY_DAYS, DEFAULT_VOUCHER_EXPIRY_DAYS);
        LocalDate expiresAt = LocalDate.now().plusDays(expiryDays);

        // Tạo voucher combo
        Voucher voucher = voucherService.createComboDiscountVoucher(user, combo, quantity, expiresAt);

        // Trừ điểm
        String desc = String.format("Đổi %d điểm lấy combo '%s' x%d (mã %s)",
                pointsNeeded, combo.getName(), quantity, voucher.getCode());
        debitPoints(user, pointsNeeded, PointsType.REDEEM, desc, null);

        // Ghi lịch sử đổi điểm
        PointRedemption redemption = PointRedemption.builder()
                .user(user)
                .pointsUsed(pointsNeeded)
                .redemptionType(RedemptionType.COMBO)
                .voucherId(voucher.getId())
                .comboId(comboId)
                .comboQuantity(quantity)
                .description(desc)
                .build();
        redemption = redemptionRepository.save(redemption);

        log.info("[Loyalty] User {} đổi {} điểm lấy combo '{}' x{} (voucher {})",
                userId, pointsNeeded, combo.getName(), quantity, voucher.getCode());
        return redemption;
    }

    // ============================================================
    // 5. ADJUSTMENT — Hoàn điểm khi hủy
    // ============================================================

    /**
     * Xử lý hoàn điểm khi booking bị hủy từ phía rạp.
     *
     * <p>Thực hiện 2 thao tác (dùng type ADJUSTMENT):
     * <ol>
     *   <li>Hoàn lại số điểm user đã tiêu (điểm loyalty dùng để giảm giá) → +điểm</li>
     *   <li>Thu hồi số điểm user đã nhận từ đơn hàng đó (EARN) → -điểm</li>
     * </ol>
     *
     * @param booking Booking vừa bị chuyển sang CANCELLED
     */
    @Transactional
    public void adjustPointsForCancelledBooking(Booking booking) {
        Integer bookingId = booking.getId();
        User user = booking.getUser();
        String bookingCode = booking.getBookingCode();

        // 1. Hoàn lại điểm đã dùng (nếu có)
        int pointsUsed = booking.getLoyaltyPointsUsed() != null ? booking.getLoyaltyPointsUsed() : 0;
        if (pointsUsed > 0) {
            String refundDesc = String.format("Hoàn %d điểm do suất chiếu #%s bị hủy",
                    pointsUsed, bookingCode);
            creditPoints(user, pointsUsed, PointsType.ADJUSTMENT, refundDesc, bookingId, null, null);
            log.info("[Loyalty] Hoàn {} điểm cho user {} (booking {} bị hủy)",
                    pointsUsed, user.getId(), bookingCode);
        }

        // 2. Thu hồi điểm EARN từ booking này (nếu đã được cộng)
        historyRepository.findByBookingIdAndPointsType(bookingId, PointsType.EARN)
                .ifPresent(earnHistory -> {
                    int earnedPoints = earnHistory.getPointsChange();
                    String clawbackDesc = String.format(
                            "Thu hồi %d điểm EARN từ đơn hàng #%s (suất chiếu bị hủy)",
                            earnedPoints, bookingCode);
                    debitPoints(user, earnedPoints, PointsType.ADJUSTMENT, clawbackDesc, bookingId);
                    log.info("[Loyalty] Thu hồi {} điểm EARN của user {} (booking {} bị hủy)",
                            earnedPoints, user.getId(), bookingCode);
                });
    }

    // ============================================================
    // PRIVATE HELPERS (mở rộng)
    // ============================================================

    /**
     * Validate điều kiện đổi điểm:
     * - User phải là CUSTOMER (không phải GUEST)
     * - Số điểm đủ để đổi
     * - Vượt mức tối thiểu
     */
    private void validateRedeemRequest(User user, int pointsToUse) {
        if (com.viecinema.common.enums.Role.GUEST.equals(user.getRole())) {
            throw new SpecificBusinessException("Khách vãng lai không thể đổi điểm.");
        }
        int minPoints = getConfigInt(KEY_REDEEM_MIN, DEFAULT_REDEEM_MIN);
        if (pointsToUse < minPoints) {
            throw new SpecificBusinessException(
                    String.format("Cần ít nhất %d điểm để đổi. Bạn đang muốn đổi %d điểm.",
                            minPoints, pointsToUse));
        }
        int currentPoints = user.getLoyaltyPoints() == null ? 0 : user.getLoyaltyPoints();
        if (currentPoints < pointsToUse) {
            throw new SpecificBusinessException(
                    String.format("Không đủ điểm. Hiện có: %d, cần: %d.", currentPoints, pointsToUse));
        }
    }

    /**
     * Trừ điểm khỏi tài khoản user và ghi lịch sử.
     * Điểm trừ không được xuống dưới 0.
     */
    private void debitPoints(User user, int points, PointsType type,
                             String description, Integer bookingId) {
        int oldBalance = user.getLoyaltyPoints() == null ? 0 : user.getLoyaltyPoints();
        int newBalance = Math.max(0, oldBalance - points);

        user.setLoyaltyPoints(newBalance);
        userRepository.save(user);

        LoyaltyPointsHistory history = LoyaltyPointsHistory.builder()
                .user(user)
                .bookingId(bookingId)
                .pointsChange(-points) // âm = trừ
                .pointsType(type)
                .description(description)
                .oldBalance(oldBalance)
                .newBalance(newBalance)
                .build();
        historyRepository.save(history);
    }
}
