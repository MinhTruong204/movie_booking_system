package com.viecinema.loyalty.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.MembershipTierRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.entity.Booking;
import com.viecinema.loyalty.entity.LoyaltyPointsConfig;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory.PointsType;
import com.viecinema.loyalty.repository.LoyaltyPointsConfigRepository;
import com.viecinema.loyalty.repository.LoyaltyPointsHistoryRepository;
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

    // Fallback defaults (nếu DB config chưa seed)
    private static final BigDecimal DEFAULT_EARN_RATE = new BigDecimal("0.0001");
    private static final int DEFAULT_REVIEW_BONUS = 20;
    private static final int DEFAULT_BIRTHDAY_BONUS = 100;
    private static final int DEFAULT_EXPIRY_MONTHS = 12;

    private final LoyaltyPointsHistoryRepository historyRepository;
    private final LoyaltyPointsConfigRepository configRepository;
    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;

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
}
