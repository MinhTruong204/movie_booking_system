package com.viecinema.booking.repository;

import com.viecinema.booking.entity.BookingVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BookingVoucherRepository extends JpaRepository<BookingVoucher, Integer> {

    List<BookingVoucher> findByBooking_Id(Integer bookingId);

    boolean existsByBooking_IdAndVoucher_Id(Integer bookingId, Integer voucherId);
}
