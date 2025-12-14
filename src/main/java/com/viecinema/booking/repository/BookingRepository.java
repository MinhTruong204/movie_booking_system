package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
