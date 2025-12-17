package com.viecinema.booking.repository;

import com.viecinema.booking.entity.BookingCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingComboRepository extends JpaRepository<BookingCombo, Integer> {

    /**
     * Lấy tất cả combo của một booking
     */
    @Query("SELECT bc FROM BookingCombo bc " +
            "LEFT JOIN FETCH bc.combo " +
            "WHERE bc.booking.id = :bookingId")
    List<BookingCombo> findByBookingIdWithCombo(@Param("bookingId") Integer bookingId);

    /**
     * Lấy combo của nhiều booking cùng lúc (để tránh N+1)
     */
    @Query("SELECT bc FROM BookingCombo bc " +
            "LEFT JOIN FETCH bc.combo " +
            "WHERE bc.booking.id IN :bookingIds")
    List<BookingCombo> findByBookingIdsWithCombo(@Param("bookingIds") List<Integer> bookingIds);
}
