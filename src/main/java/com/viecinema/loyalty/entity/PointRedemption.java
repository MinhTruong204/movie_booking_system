package com.viecinema.loyalty.entity;

import com.viecinema.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Ánh xạ bảng loyalty_point_redemptions (v11).
 * Ghi lại mỗi lần user đổi điểm lấy voucher hoặc combo.
 */
@Entity
@Table(name = "loyalty_point_redemptions", indexes = {
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_type", columnList = "redemption_type"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointRedemption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "redemption_id")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Số điểm đã tiêu cho lần đổi này. */
    @Column(name = "points_used", nullable = false)
    private Integer pointsUsed;

    @Enumerated(EnumType.STRING)
    @Column(name = "redemption_type", nullable = false)
    private RedemptionType redemptionType;

    /** ID voucher được tạo ra từ lần đổi này. */
    @Column(name = "voucher_id")
    private Integer voucherId;

    /** ID combo được đổi (nếu type=COMBO). */
    @Column(name = "combo_id")
    private Integer comboId;

    @Column(name = "combo_quantity")
    private Integer comboQuantity = 1;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum RedemptionType {
        VOUCHER,  // Đổi lấy voucher giảm tiền vé
        COMBO     // Đổi lấy combo bắp nước
    }
}
