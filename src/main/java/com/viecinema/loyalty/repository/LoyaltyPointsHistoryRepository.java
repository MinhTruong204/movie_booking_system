package com.viecinema.loyalty.repository;

import com.viecinema.loyalty.entity.LoyaltyPointsHistory;
import com.viecinema.loyalty.entity.LoyaltyPointsHistory.PointsType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoyaltyPointsHistoryRepository extends JpaRepository<LoyaltyPointsHistory, Integer> {

    /**
     * Kiểm tra đã EARN cho booking này chưa — chống duplicate.
     */
    boolean existsByBookingIdAndPointsType(Integer bookingId, PointsType pointsType);

    /**
     * Tìm bản ghi lịch sử điểm theo bookingId và type.
     * Dùng để tìm record EARN khi cần thu hồi điểm lúc hủy booking.
     */
    java.util.Optional<LoyaltyPointsHistory> findByBookingIdAndPointsType(Integer bookingId, PointsType pointsType);

    /**
     * Kiểm tra đã BONUS cho review này chưa — chống duplicate.
     */
    boolean existsByReviewId(Integer reviewId);

    /**
     * Kiểm tra đã tặng điểm sinh nhật cho user này trong khoảng thời gian chưa.
     * Dùng để chống tặng trùng năm: truyền đầu năm và cuối năm.
     */
    boolean existsByUser_IdAndPointsTypeAndDescriptionContainingAndCreatedAtBetween(
            Integer userId,
            PointsType pointsType,
            String descriptionKeyword,
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Lịch sử điểm của user — phân trang, mới nhất trước.
     */
    Page<LoyaltyPointsHistory> findByUser_IdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    /**
     * Lấy tất cả lịch sử điểm của user (không phân trang, dùng cho export).
     */
    List<LoyaltyPointsHistory> findByUser_IdOrderByCreatedAtDesc(Integer userId);

    /**
     * Điểm sắp hết hạn của user (để hiển thị cảnh báo).
     */
    @Query("""
            SELECT h FROM LoyaltyPointsHistory h
            WHERE h.user.id = :userId
              AND h.pointsType = 'EARN'
              AND h.expiresAt IS NOT NULL
              AND h.expiresAt <= :expiryDate
            ORDER BY h.expiresAt ASC
            """)
    List<LoyaltyPointsHistory> findExpiringPoints(
            @Param("userId") Integer userId,
            @Param("expiryDate") java.time.LocalDate expiryDate
    );
}
