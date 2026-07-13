package com.viecinema.booking.repository;

import com.viecinema.booking.entity.VoucherUsageHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoucherUsageHistoryRepository extends JpaRepository<VoucherUsageHistory, Integer> {

    List<VoucherUsageHistory> findByVoucher_Id(Integer voucherId);

    boolean existsByVoucher_IdAndBookingId(Integer voucherId, Integer bookingId);
}
