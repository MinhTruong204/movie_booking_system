package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.BookingCombo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingComboRepository extends JpaRepository<BookingCombo, Integer> {

    @Query("SELECT bc FROM BookingCombo bc " +
            "LEFT JOIN FETCH bc.combo " +
            "WHERE bc.booking.id IN :bookingIds")
    List<BookingCombo> findByBookingIdsWithCombo(@Param("bookingIds") List<Integer> bookingIds);

    @Query("SELECT bc FROM BookingCombo bc LEFT JOIN FETCH bc.combo WHERE bc.booking = :booking")
    List<BookingCombo> findByBooking(@Param("booking") Booking booking);
}

