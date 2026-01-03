package com.viecinema.user.service;

import com.viecinema.auth.entity.MembershipTier;
import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.MembershipTierRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.user.dto.MembershipInfo;
import com.viecinema.user.dto.UserProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final UserStatisticsService userStatisticsService;

    /**
     * Lấy thông tin profile của user hiện tại
     *
     * @param userId ID của user đang đăng nhập (lấy từ JWT)
     * @return UserProfileDto
     * @throws ResourceNotFoundException nếu không tìm thấy user
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "userProfile", key = "#userId", unless = "#result == null")
    public UserProfileDto getUserProfile(Integer userId) {
        log.info("Fetching profile for user ID: {}", userId);

        // Lấy user với membership tier (eager loading)
        User user = userRepository.findActiveUserWithMembership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User" + userId));

        // Build DTO
        return UserProfileDto.builder()
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .gender(user.getGender() != null ? user.getGender().name() : null)
                .birthDate(user.getBirthDate())
                .role(user.getRole().name())
                .isActive(user.getIsActive())
                .emailVerified(user.getEmailVerified())
                .phoneVerified(user.getPhoneVerified())
                .membership(buildMembershipInfo(user))
                .statistics(userStatisticsService.getUserStatistics(userId))
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Build thông tin membership của user
     */
    private MembershipInfo buildMembershipInfo(User user) {
        MembershipTier currentTier = user.getMembershipTier();
        Integer currentPoints = user.getLoyaltyPoints();

        // Tìm hạng tiếp theo
        List<MembershipTier> nextTiers = membershipTierRepository.findNextTier(currentPoints);
        Optional<MembershipTier> nextTierOpt = nextTiers.isEmpty() ? Optional.empty() : Optional.of(nextTiers.get(0));

        Integer pointsToNextTier = null;
        Integer nextTierPoints = null;
        Double progressPercent = 0.0;

        if (nextTierOpt.isPresent()) {
            MembershipTier nextTier = nextTierOpt.get();
            nextTierPoints = nextTier.getPointsRequired();
            pointsToNextTier = nextTierPoints - currentPoints;

            // Tính % progress
            int pointsInCurrentRange = nextTierPoints - currentTier.getPointsRequired();
            int pointsEarnedInRange = currentPoints - currentTier.getPointsRequired();

            if (pointsInCurrentRange > 0) {
                progressPercent = (pointsEarnedInRange * 100.0) / pointsInCurrentRange;
                progressPercent = Math.max(0, Math.min(100, progressPercent)); // Clamp 0-100
            }
        } else {
            // Đã đạt hạng cao nhất
            progressPercent = 100.0;
        }

        // Tính số vé free còn lại trong năm
        Integer freeTicketsRemaining = calculateFreeTicketsRemaining(user);

        return MembershipInfo.builder()
                .tierId(currentTier.getId())
                .tierName(currentTier.getName())
                .tierDescription(currentTier.getDescription())
                .colorCode(currentTier.getColorCode())
                .iconUrl(currentTier.getIconUrl())
                .currentPoints(currentPoints)
                .pointsRequired(nextTierPoints)
                .pointsToNextTier(pointsToNextTier)
                .discountPercent(currentTier.getDiscountPercent())
                .birthdayDiscount(currentTier.getBirthdayDiscount())
                .freeTicketsPerYear(currentTier.getFreeTicketsPerYear())
                .freeTicketsRemaining(freeTicketsRemaining)
                .priorityBooking(currentTier.getPriorityBooking())
                .memberSince(user.getMemberSince())
                .progressPercent(Math.round(progressPercent * 100.0) / 100.0) // Round 2 số thập phân
                .build();
    }

    /**
     * Tính số vé miễn phí còn lại trong năm
     * Logic: Đếm số booking đã dùng free ticket trong năm hiện tại
     * <p>
     * TODO: Cần có flag trong bảng bookings để đánh dấu booking dùng free ticket
     * Tạm thời return giá trị mặc định
     */
    private Integer calculateFreeTicketsRemaining(User user) {
        Integer totalFreeTickets = user.getMembershipTier().getFreeTicketsPerYear();

        if (totalFreeTickets == null || totalFreeTickets == 0) {
            return 0;
        }

        // TODO: Query database để đếm số free ticket đã dùng trong năm
        // int usedThisYear = bookingRepository.countFreeTicketsUsedThisYear(user. getUserId(), LocalDate.now(). getYear());
        // return totalFreeTickets - usedThisYear;

        // Tạm thời return full
        return totalFreeTickets;
    }
}
