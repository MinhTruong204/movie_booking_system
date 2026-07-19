package com.viecinema.loyalty.service;

import com.viecinema.auth.entity.MembershipTier;
import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.MembershipTierRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.loyalty.dto.LoyaltyHistoryItemDto;
import com.viecinema.loyalty.dto.LoyaltySummaryDto;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory.PointsType;
import com.viecinema.loyalty.mapper.LoyaltyMapper;
import com.viecinema.loyalty.repository.LoyaltyPointsHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service xử lý các query read-only liên quan đến loyalty points.
 * Tách khỏi {@link LoyaltyPointsService} để tuân theo Single Responsibility.
 */
@Service
@RequiredArgsConstructor
public class LoyaltyQueryService {

    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final LoyaltyPointsHistoryRepository historyRepository;
    private final LoyaltyMapper loyaltyMapper;

    /**
     * Trả về thông tin tổng hợp điểm của user.
     */
    @Transactional(readOnly = true)
    public LoyaltySummaryDto getSummary(Integer userId) {
        User user = userRepository.findActiveUserWithMembership(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + userId));

        int currentPoints = user.getLoyaltyPoints() == null ? 0 : user.getLoyaltyPoints();
        MembershipTier currentTier = user.getMembershipTier();

        // Tính điểm cần để lên hạng tiếp
        List<MembershipTier> nextTiers = membershipTierRepository.findNextTier(currentPoints);
        MembershipTier nextTier = nextTiers.isEmpty() ? null : nextTiers.get(0);

        // Tổng điểm đã tích / đã tiêu từ lịch sử
        List<LoyaltyPointsHistory> allHistory = historyRepository.findByUser_IdOrderByCreatedAtDesc(userId);

        long totalEarned = allHistory.stream()
                .filter(h -> h.getPointsChange() > 0)
                .mapToLong(h -> h.getPointsChange())
                .sum();

        long totalRedeemed = allHistory.stream()
                .filter(h -> h.getPointsType() == PointsType.REDEEM)
                .mapToLong(h -> Math.abs(h.getPointsChange()))
                .sum();

        return LoyaltySummaryDto.builder()
                .userId(userId)
                .currentPoints(currentPoints)
                .membershipTierName(currentTier != null ? currentTier.getName() : "Member")
                .tierColorCode(currentTier != null ? currentTier.getColorCode() : "#C0C0C0")
                .pointsToNextTier(nextTier != null
                        ? nextTier.getPointsRequired() - currentPoints
                        : null)
                .nextTierName(nextTier != null ? nextTier.getName() : null)
                .totalPointsEarned(totalEarned)
                .totalPointsRedeemed(totalRedeemed)
                .totalSpent(user.getTotalSpent())
                .build();
    }

    /**
     * Trả về lịch sử điểm của user — phân trang, mới nhất trước.
     */
    @Transactional(readOnly = true)
    public Page<LoyaltyHistoryItemDto> getHistory(Integer userId, Pageable pageable) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User không tồn tại: " + userId);
        }
        return historyRepository
                .findByUser_IdOrderByCreatedAtDesc(userId, pageable)
                .map(loyaltyMapper::toHistoryItemDto);
    }

    /**
     * Tải thông tin user (dùng để lấy số dư điểm sau khi redeem).
     */
    @Transactional(readOnly = true)
    public User getUserById(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + userId));
    }
}
