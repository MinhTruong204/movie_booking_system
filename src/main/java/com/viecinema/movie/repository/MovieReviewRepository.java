package com.viecinema.movie.repository;

import com.viecinema.movie.entity.MovieReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieReviewRepository extends JpaRepository<MovieReview, Integer> {

    boolean existsByMovie_MovieIdAndUser_Id(Integer movieId, Integer userId);

    Optional<MovieReview> findByMovie_MovieIdAndUser_Id(Integer movieId, Integer userId);

    Page<MovieReview> findByMovie_MovieIdAndIsApprovedTrueOrderByCreatedAtDesc(
            Integer movieId, Pageable pageable);

    /**
     * Kiểm tra user có booking PAID cho phim này không — dùng để set is_verified_booking.
     * BookingStatus.PAID so sánh dạng string với ENUM trong JPA.
     */
    @Query("""
            SELECT COUNT(b) > 0
            FROM Booking b
            JOIN b.showtime s
            WHERE b.user.id = :userId
              AND s.movie.movieId = :movieId
              AND b.status = com.viecinema.common.enums.BookingStatus.PAID
              AND b.deletedAt IS NULL
            """)
    boolean hasVerifiedBooking(
            @Param("userId") Integer userId,
            @Param("movieId") Integer movieId
    );

    /**
     * Tính average rating của phim dựa trên tất cả review đã được approved.
     * Trả về null nếu chưa có review nào.
     */
    @Query("""
            SELECT AVG(r.rating)
            FROM MovieReview r
            WHERE r.movie.movieId = :movieId
              AND r.isApproved = true
              AND r.deletedAt IS NULL
            """)
    Double calculateAverageRating(@Param("movieId") Integer movieId);

    /**
     * Đếm tổng số review đã được approved của phim.
     */
    @Query("""
            SELECT COUNT(r)
            FROM MovieReview r
            WHERE r.movie.movieId = :movieId
              AND r.isApproved = true
              AND r.deletedAt IS NULL
            """)
    Long countApprovedReviews(@Param("movieId") Integer movieId);
}

