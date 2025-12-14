package com.viecinema.showtime.entity;

import com.viecinema.auth.entity.User;
import com.viecinema.common.entity.BaseEntity;
import com.viecinema.common.enums.SeatStatusType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatStatus extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_status_id")
    private Integer seatStatusId;

    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private SeatStatusType status = SeatStatusType.AVAILABLE;

    @ManyToOne(fetch = FetchType. LAZY)
    @JoinColumn(name = "held_by_user_id")
    private User heldByUser;

    @Column(name = "held_until")
    private LocalDateTime heldUntil;

    @Version
    @Column(name = "version")
    @Builder.Default
    private Integer version = 0;

    /**
     * Kiểm tra ghế có thể đặt được không
     */
    public boolean isAvailable() {
        if (status == SeatStatusType.AVAILABLE) {
            return true;
        }
        // Nếu đang held nhưng đã hết hạn -> available
        if (status == SeatStatusType.HELD && heldUntil != null && heldUntil.isBefore(LocalDateTime.now())) {
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra ghế đang được held bởi user cụ thể
     */
    public boolean isHeldBy(Integer userId) {
        return status == SeatStatusType.HELD
                && heldByUser != null
                && heldByUser.getId(). equals(userId)
                && heldUntil != null
                && heldUntil. isAfter(LocalDateTime.now());
    }

    /**
     * Tính thời gian còn lại (giây) nếu đang held
     */
    public Long getRemainingHoldSeconds() {
        if (status != SeatStatusType.HELD || heldUntil == null) {
            return null;
        }
        long seconds = java.time.Duration. between(LocalDateTime.now(), heldUntil). getSeconds();
        return seconds > 0 ? seconds : null;
    }
}