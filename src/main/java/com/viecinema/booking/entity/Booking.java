package com.viecinema.booking.entity;

import com.viecinema.auth.entity.User;
import com.viecinema.common.entity.DeletableEntity;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.showtime.entity.Showtime;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_user", columnList = "user_id"),
        @Index(name = "idx_showtime", columnList = "showtime_id"),
        @Index(name = "idx_booking_code", columnList = "booking_code"),
        @Index(name = "idx_status", columnList = "status")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @Size(max = 20)
    @NotNull
    @Column(name = "booking_code", nullable = false, length = 20)
    private String bookingCode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BookingStatus status;

    @NotNull
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @NotNull
    @Column(name = "final_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalAmount;

    @Size(max = 255)
    @Column(name = "qr_code_data")
    private String qrCodeData;

    @Size(max = 255)
    @Column(name = "qr_code_image_url")
    private String qrCodeImageUrl;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "checked_in_by")
    private User checkedInBy;

    @Size(max = 100)
    @Column(name = "checked_in_location", length = 100)
    private String checkedInLocation;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<BookingSeat> bookingSeats;
}
