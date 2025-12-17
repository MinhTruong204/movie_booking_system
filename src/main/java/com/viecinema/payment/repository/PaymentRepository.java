package com.viecinema.payment.repository;

import com.viecinema.booking.entity.BookingCombo;
import com.viecinema.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Payment findByTransactionId(String transactionId);
}
