package com.viecinema.booking.repository;

import com.viecinema.booking.entity.BookingPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingPromotionRepository extends JpaRepository<BookingPromotion, Integer> {

    List<BookingPromotion> findByBookingId(Integer bookingId);
}
