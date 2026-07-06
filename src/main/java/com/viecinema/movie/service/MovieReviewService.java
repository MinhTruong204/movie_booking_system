package com.viecinema.movie.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.exception.SpecificBusinessException;
import com.viecinema.loyalty.service.LoyaltyPointsService;
import com.viecinema.movie.dto.request.CreateReviewRequest;
import com.viecinema.movie.dto.response.ReviewResponse;
import com.viecinema.movie.entity.Movie;
import com.viecinema.movie.entity.MovieReview;
import com.viecinema.movie.mapper.ReviewMapper;
import com.viecinema.movie.repository.MovieRepository;
import com.viecinema.movie.repository.MovieReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service xử lý nghiệp vụ đánh giá phim (Movie Review).
 *
 * <p>Khi user tạo review, hệ thống tự động kiểm tra xem user có booking PAID
 * cho phim đó không. Nếu có, {@code is_verified_booking = TRUE} và
 * {@link LoyaltyPointsService#awardReviewBonus} được gọi để cộng điểm BONUS.
 *
 * <p>Cả GUEST và CUSTOMER đều có thể tạo review nếu đã đặt vé.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MovieReviewService {

    private final MovieReviewRepository reviewRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;
    private final LoyaltyPointsService loyaltyPointsService;
    private final ReviewMapper reviewMapper;

    /**
     * Tạo review mới cho phim.
     *
     * <ol>
     *   <li>Kiểm tra phim tồn tại</li>
     *   <li>Kiểm tra user chưa review phim này (UNIQUE constraint)</li>
     *   <li>Xác minh booking — nếu có booking PAID → is_verified_booking = TRUE</li>
     *   <li>Lưu review</li>
     *   <li>Nếu is_verified_booking = TRUE → cộng điểm BONUS</li>
     * </ol>
     *
     * @param userId  ID của user đang đăng nhập
     * @param request Request body
     * @return ReviewResponse bao gồm bonusPointsAwarded nếu có
     */
    @Transactional
    public ReviewResponse createReview(Integer userId, CreateReviewRequest request) {
        Movie movie = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Phim không tồn tại: " + request.getMovieId()));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User không tồn tại: " + userId));

        // Kiểm tra user chưa review phim này
        if (reviewRepository.existsByMovie_MovieIdAndUser_Id(request.getMovieId(), userId)) {
            throw new SpecificBusinessException("Bạn đã đánh giá phim này rồi.");
        }

        // Xác minh booking — kiểm tra có booking PAID cho phim không
        boolean isVerified = reviewRepository.hasVerifiedBooking(userId, request.getMovieId());

        MovieReview review = MovieReview.builder()
                .movie(movie)
                .user(user)
                .rating(request.getRating())
                .ratingVideo(request.getRatingVideo())
                .ratingAudio(request.getRatingAudio())
                .ratingSubtitle(request.getRatingSubtitle())
                .comment(request.getComment())
                .isVerifiedBooking(isVerified)
                .build();

        review = reviewRepository.save(review);

        // Cộng điểm BONUS nếu là review đã xác thực
        Integer bonusPoints = null;
        if (isVerified) {
            try {
                loyaltyPointsService.awardReviewBonus(userId, review.getId());
                bonusPoints = loyaltyPointsService.getReviewBonusPoints();
                log.info("[Review] User {} nhận {} điểm BONUS cho review phim {} (review #{})",
                        userId, bonusPoints, request.getMovieId(), review.getId());
            } catch (Exception e) {
                // Lỗi tích điểm không làm hỏng review — log và tiếp tục
                log.error("[Review] Lỗi khi cộng điểm BONUS cho review #{}: {}", review.getId(), e.getMessage(), e);
            }
        }

        // Chuyển entity → DTO qua mapper, sau đó set bonusPointsAwarded
        ReviewResponse response = reviewMapper.toReviewResponse(review);
        if (bonusPoints != null) {
            response.setBonusPointsAwarded(bonusPoints);
        }
        return response;
    }

    /**
     * Lấy danh sách review của phim — phân trang, mới nhất trước.
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByMovie(Integer movieId, Pageable pageable) {
        if (!movieRepository.existsById(movieId)) {
            throw new ResourceNotFoundException("Phim không tồn tại: " + movieId);
        }
        return reviewRepository
                .findByMovie_MovieIdAndIsApprovedTrueOrderByCreatedAtDesc(movieId, pageable)
                .map(reviewMapper::toReviewResponse);
    }
}
