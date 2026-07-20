package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Booking;
import com.viecinema.common.enums.BookingStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    @EntityGraph(attributePaths = {
            "showtime.movie",
            "showtime.room.cinema",
            "bookingSeats.seat.seatType",
            "user"
    })
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    List<Booking> findAllByUserIdWithDetails(@Param("userId") Integer userId);

    @Modifying(clearAutomatically = true) // Reset cache after update
    @Query("UPDATE Booking b SET b.status = :newStatus " +
            "WHERE b.status = :oldStatus AND b.createdAt < :expirationTime")
    int updateStatusForExpiredBookings(
            @Param("newStatus") BookingStatus newStatus,
            @Param("oldStatus") BookingStatus oldStatus,
            @Param("expirationTime") LocalDateTime expirationTime
    );
    @EntityGraph(attributePaths = {
            "showtime",
            "bookingSeats.seat"
    })
    @Query("SELECT b FROM Booking b WHERE b.status = :status AND b.createdAt < :expirationTime")
    List<Booking> findExpiredBookings(
            @Param("status") BookingStatus status,
            @Param("expirationTime") LocalDateTime expirationTime
    );

    /**
     * Load booking với đầy đủ associations cần thiết để gửi email xác nhận.
     * Sử dụng EntityGraph để tránh LazyInitializationException khi chạy @Async.
     */
    @EntityGraph(attributePaths = {
            "user",
            "showtime.movie",
            "showtime.room.cinema",
            "bookingSeats.seat.seatType"
    })
    @Query("SELECT b FROM Booking b WHERE b.id = :id")
    Optional<Booking> findByIdForEmail(@Param("id") Integer id);

    /**
     * Load một booking với đầy đủ thông tin để trả về chi tiết vé.
     * EntityGraph đảm bảo tất cả associations được eager-load trong một query,
     * tránh LazyInitializationException và N+1 problem.
     */
    @EntityGraph(attributePaths = {
            "user",
            "showtime.movie",
            "showtime.room.cinema",
            "bookingSeats.seat.seatType"
    })
    @Query("SELECT b FROM Booking b WHERE b.id = :bookingId AND b.deletedAt IS NULL")
    Optional<Booking> findByIdWithFullDetails(@Param("bookingId") Integer bookingId);
}
