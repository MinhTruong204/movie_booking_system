package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    Optional<Booking> findByBookingCode(String bookingCode);

    @Query("SELECT b FROM Booking b " +
            "WHERE b.user.id = :userId " +
            "AND b.deletedAt IS NULL " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findByUserId(@Param("userId") Integer userId);

    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.user.id = :userId " +
            "AND b.status = com.viecinema.common.enums.BookingStatus.PAID")
    long countPaidBookingsByUserId(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.showtime st " +
            "LEFT JOIN FETCH st.movie m " +
            "LEFT JOIN FETCH st.room r " +
            "LEFT JOIN FETCH r.cinema c " +
            "LEFT JOIN FETCH b.bookingSeats bs " +
            "LEFT JOIN FETCH bs.seat s " +
            "LEFT JOIN FETCH s.seatType " +
            "WHERE b.user.id = :userId " +
            "AND b.deletedAt IS NULL " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findAllByUserIdWithDetails(@Param("userId") Integer userId);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.showtime st " +
            "LEFT JOIN FETCH st.movie m " +
            "LEFT JOIN FETCH st.room r " +
            "LEFT JOIN FETCH r.cinema c " +
            "LEFT JOIN FETCH b.bookingSeats bs " +
            "LEFT JOIN FETCH bs.seat s " +
            "LEFT JOIN FETCH s.seatType " +
            "WHERE b.user.id = :userId " +
            "AND b.status = :status " +
            "AND b.deletedAt IS NULL " +
            "ORDER BY b.createdAt DESC")
    List<Booking> findByUserIdAndStatus(@Param("userId") Integer userId,
                                        @Param("status") String status);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.showtime st " +
            "LEFT JOIN FETCH st.movie m " +
            "LEFT JOIN FETCH st.room r " +
            "LEFT JOIN FETCH r.cinema c " +
            "LEFT JOIN FETCH b.bookingSeats bs " +
            "LEFT JOIN FETCH bs.seat s " +
            "LEFT JOIN FETCH s.seatType " +
            "WHERE b.user.id = :userId " +
            "AND b.status = 'paid' " +
            "AND st.startTime > :now " +
            "AND b.deletedAt IS NULL " +
            "ORDER BY st.startTime ASC")
    List<Booking> findUpcomingBookings(@Param("userId") Integer userId,
                                       @Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT b FROM Booking b " +
            "LEFT JOIN FETCH b.showtime st " +
            "LEFT JOIN FETCH st.movie m " +
            "LEFT JOIN FETCH st.room r " +
            "LEFT JOIN FETCH r.cinema c " +
            "LEFT JOIN FETCH b.bookingSeats bs " +
            "LEFT JOIN FETCH bs.seat s " +
            "LEFT JOIN FETCH s.seatType " +
            "WHERE b.user.id = :userId " +
            "AND st.endTime < :now " +
            "AND b.deletedAt IS NULL " +
            "ORDER BY st.startTime DESC")
    List<Booking> findPastBookings(@Param("userId") Integer userId,
                                   @Param("now") LocalDateTime now);

    @Query("SELECT b FROM Booking b " +
            "LEFT JOIN FETCH b.showtime st " +
            "LEFT JOIN FETCH st.movie " +
            "LEFT JOIN FETCH st.room r " +
            "LEFT JOIN FETCH r.cinema " +
            "WHERE b.bookingCode = :bookingCode " +
            "AND b.deletedAt IS NULL")
    Optional<Booking> findByBookingCodeWithDetails(@Param("bookingCode") String bookingCode);

    @Query("SELECT COUNT(b) FROM Booking b " +
            "WHERE b.user.id = :userId " +
            "AND b.status = :status " +
            "AND b.deletedAt IS NULL")
    Long countByUserIdAndStatus(@Param("userId") Integer userId,
                                @Param("status") String status);
}
