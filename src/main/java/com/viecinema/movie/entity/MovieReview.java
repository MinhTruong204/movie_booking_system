package com.viecinema.movie.entity;

import com.viecinema.auth.entity.User;
import com.viecinema.common.entity.DeletableEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Ánh xạ bảng movie_reviews.
 * Khi is_verified_booking = TRUE và review được tạo thành công,
 * hệ thống sẽ tự động cộng điểm BONUS qua LoyaltyPointsService.
 */
@Entity
@Table(name = "movie_reviews",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_movie_user",
                columnNames = {"movie_id", "user_id"}
        ))
@SQLDelete(sql = "UPDATE movie_reviews SET deleted_at = NOW() WHERE review_id = ?")
@Where(clause = "deleted_at IS NULL")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieReview extends DeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** booking_id dùng để xác minh user đã xem phim */
    @Column(name = "booking_id")
    private Integer bookingId;

    @Column(name = "rating", nullable = false)
    private Integer rating;

    @Column(name = "rating_video")
    private Integer ratingVideo;

    @Column(name = "rating_audio")
    private Integer ratingAudio;

    @Column(name = "rating_subtitle")
    private Integer ratingSubtitle;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    /**
     * TRUE khi hệ thống đã xác minh user có booking PAID cho phim này.
     * Khi trường này được set TRUE, LoyaltyPointsService sẽ tự động
     * cộng điểm BONUS cho user.
     */
    @Column(name = "is_verified_booking")
    @Builder.Default
    private Boolean isVerifiedBooking = false;

    @Column(name = "is_approved")
    @Builder.Default
    private Boolean isApproved = true;

    @Column(name = "helpful_count")
    @Builder.Default
    private Integer helpfulCount = 0;

    @Column(name = "not_helpful_count")
    @Builder.Default
    private Integer notHelpfulCount = 0;
}
