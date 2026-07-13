package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Voucher;
import com.viecinema.booking.entity.Voucher.VoucherStatus;
import com.viecinema.booking.entity.Voucher.VoucherType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {

    Optional<Voucher> findByCode(String code);

    /**
     * Lấy danh sách voucher active của một user (dùng để hiển thị ví voucher).
     * Chỉ trả về các voucher chưa hết hạn.
     */
    @Query("""
            SELECT v FROM Voucher v
            WHERE v.owner.id = :userId
              AND v.status = 'ACTIVE'
              AND v.expiresAt >= :today
            ORDER BY v.expiresAt ASC
            """)
    List<Voucher> findActiveVouchersByOwner(
            @Param("userId") Integer userId,
            @Param("today") LocalDate today
    );

    /**
     * Lấy voucher active của user theo từng loại.
     */
    @Query("""
            SELECT v FROM Voucher v
            WHERE v.owner.id = :userId
              AND v.voucherType = :type
              AND v.status = 'ACTIVE'
              AND v.expiresAt >= :today
            ORDER BY v.expiresAt ASC
            """)
    List<Voucher> findActiveVouchersByOwnerAndType(
            @Param("userId") Integer userId,
            @Param("type") VoucherType type,
            @Param("today") LocalDate today
    );
}
