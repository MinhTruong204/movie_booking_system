package com.viecinema.payment.repository;

import com.viecinema.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Payment findByTransactionId(String transactionId);

    /**
     * Tìm payment theo booking ID
     */
    Optional<Payment> findByBooking_Id(Integer bookingId);

    /**
     * Tìm payment của nhiều booking
     */
    @Query("SELECT p FROM Payment p WHERE p.booking.id IN :bookingIds")
    List<Payment> findByBookingIds(@Param("bookingIds") List<Integer> bookingIds);
}
