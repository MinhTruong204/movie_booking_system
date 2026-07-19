package com.viecinema.movie.service;

import com.viecinema.movie.entity.MovieStatistic;
import com.viecinema.movie.repository.MovieReviewRepository;
import com.viecinema.movie.repository.MovieStatisticsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Service cap nhat cac chi so thong ke phim trong bang movie_statistics.
 * Duoc goi sau khi:
 *   - Booking duoc thanh toan thanh cong
 *   - User gui review
 *
 * Khong dung Propagation.REQUIRES_NEW de tranh LazyInitializationException
 * voi lazy-loaded entity. Thay vao do nhan primitive values tu caller.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieStatisticsService {

    private final MovieStatisticsRepository statisticsRepository;
    private final MovieReviewRepository reviewRepository;

    /**
     * Cap nhat thong ke sau khi booking duoc thanh toan thanh cong.
     * Caller phai truyen gia tri primitive de tranh lazy loading issue.
     *
     * @param movieId     ID phim
     * @param seatsCount  So ghe da dat
     * @param finalAmount Tong tien da thanh toan
     */
    @Transactional
    public void onBookingPaid(Integer movieId, int seatsCount, BigDecimal finalAmount) {
        log.info("[MovieStats] Updating booking stats for movie {}", movieId);

        MovieStatistic stat = statisticsRepository.findById(movieId).orElse(null);
        if (stat == null) {
            log.warn("[MovieStats] Khong tim thay MovieStatistic cho movieId={}", movieId);
            return;
        }

        stat.setTotalBookings(nullSafe(stat.getTotalBookings()) + 1);
        stat.setTotalSeatsSold(nullSafe(stat.getTotalSeatsSold()) + seatsCount);
        stat.setTotalRevenue(nullSafeDec(stat.getTotalRevenue()).add(finalAmount));
        stat.setLastUpdated(Instant.now());

        statisticsRepository.save(stat);
        log.info("[MovieStats] Movie {} - bookings={}, seatsSold={}, revenue={}",
                movieId, stat.getTotalBookings(), stat.getTotalSeatsSold(), stat.getTotalRevenue());
    }

    /**
     * Cap nhat thong ke sau khi user gui review cho phim.
     * Tinh lai average_rating va total_reviews tu DB.
     *
     * @param movieId ID cua phim vua nhan duoc review moi
     */
    @Transactional
    public void onReviewCreated(Integer movieId) {
        log.info("[MovieStats] Updating review stats for movie {}", movieId);

        MovieStatistic stat = statisticsRepository.findById(movieId).orElse(null);
        if (stat == null) {
            log.warn("[MovieStats] Khong tim thay MovieStatistic cho movieId={}", movieId);
            return;
        }

        Double avg = reviewRepository.calculateAverageRating(movieId);
        Long count = reviewRepository.countApprovedReviews(movieId);

        stat.setTotalReviews(count != null ? count.intValue() : 0);
        stat.setAverageRating(avg != null
                ? BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        stat.setLastUpdated(Instant.now());

        statisticsRepository.save(stat);
        log.info("[MovieStats] Movie {} - totalReviews={}, averageRating={}",
                movieId, stat.getTotalReviews(), stat.getAverageRating());
    }

    // ========== HELPERS ==========

    private int nullSafe(Integer value) {
        return value != null ? value : 0;
    }

    private BigDecimal nullSafeDec(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}