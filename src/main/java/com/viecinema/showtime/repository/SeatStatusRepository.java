package com.viecinema.showtime.repository;

import com.viecinema.showtime.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatStatusRepository extends JpaRepository<SeatStatus, Integer> {

    /**
     * Lấy tất cả trạng thái ghế của một suất chiếu
     */
    @Query("""
        SELECT ss FROM SeatStatus ss
        JOIN FETCH ss.seat s
        JOIN FETCH s.seatType st
        LEFT JOIN FETCH ss.heldByUser u
        WHERE ss.showtime.id = :showtimeId
    """)
    List<SeatStatus> findByShowtimeId(@Param("showtimeId") Integer showtimeId);

    /**
     * Lấy trạng thái một ghế cụ thể
     */
    @Query("""
        SELECT ss FROM SeatStatus ss
        WHERE ss.showtime.id = :showtimeId
        AND ss.seat.seatId = :seatId
    """)
    Optional<SeatStatus> findByShowtimeIdAndSeatId(
            @Param("showtimeId") Integer showtimeId,
            @Param("seatId") Integer seatId
    );

    /**
     * Đếm ghế theo trạng thái
     */
    @Query("""
        SELECT ss.status, COUNT(ss)
        FROM SeatStatus ss
        WHERE ss.showtime.id = :showtimeId
        GROUP BY ss.status
    """)
    List<Object[]> countByShowtimeIdGroupByStatus(@Param("showtimeId") Integer showtimeId);

    /**
     * Giải phóng các ghế held đã hết hạn
     */
    @Modifying
    @Query("""
        UPDATE SeatStatus ss
        SET ss.status = 'AVAILABLE',
            ss.heldByUser = null,
            ss.heldUntil = null,
            ss. updatedAt = CURRENT_TIMESTAMP
        WHERE ss.status = 'HELD'
        AND ss. heldUntil < :now
    """)
    int releaseExpiredHeldSeats(@Param("now") LocalDateTime now);

    /**
     * Kiểm tra xem đã có seat_status cho showtime chưa
     */
    @Query("SELECT COUNT(ss) > 0 FROM SeatStatus ss WHERE ss.showtime.id = :showtimeId")
    boolean existsByShowtimeId(@Param("showtimeId") Integer showtimeId);

    /**
     * Tìm seat status với Pessimistic Write Lock
     * Dùng cho hold seats để tránh race condition
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM SeatStatus ss WHERE ss.showtime.id = :showtimeId AND ss.seat.seatId IN :seatIds")
    List<SeatStatus> findByShowtimeAndSeatsWithLock(
            @Param("showtimeId") Integer showtimeId,
            @Param("seatIds") List<Integer> seatIds);

    /**
     * Tìm tất cả ghế của user đang giữ
     */
    @Query("SELECT ss FROM SeatStatus ss WHERE ss.heldByUser.id = :userId AND ss.status = 'held'")
    List<SeatStatus> findAllHeldByUser(@Param("userId") Integer userId);

    /**
     * Đếm số ghế user đang giữ
     */
    @Query("SELECT COUNT(ss) FROM SeatStatus ss WHERE ss.heldByUser.id = :userId AND ss.status = 'held'")
    int countHeldSeatsByUser(@Param("userId") Integer userId);

    /**
     * Release ghế hết hạn (chạy bằng scheduler)
     */
    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = 'available', ss.heldByUser.id = NULL, " +
            "ss.heldUntil = NULL WHERE ss.status = 'held' AND ss. heldUntil < : currentTime")
    int releaseExpiredSeats(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Release tất cả ghế của user (khi user rời trang)
     */
    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = 'available', ss.heldByUser.id = NULL, " +
            "ss.heldUntil = NULL WHERE ss.heldByUser.id = :userId AND ss.status = 'held'")
    int releaseUserSeats(@Param("userId") Integer userId);

    /**
     * Release a single seat only if it's currently held by the given user.
     * Returns number of rows updated (0 if not released).
     */
    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = 'available', ss.heldByUser.id = NULL, ss.heldUntil = NULL, ss.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ss.showtime.id = :showtimeId AND ss.seat.seatId = :seatId AND ss.status = 'held' AND ss.heldByUser.id = :userId")
    int releaseSeatByUser(@Param("showtimeId") Integer showtimeId,
                          @Param("seatId") Integer seatId,
                          @Param("userId") Integer userId);

    /**
     * Force release a single seat regardless who held it (admin/system).
     */
    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = 'available', ss.heldByUser.id = NULL, ss.heldUntil = NULL, ss.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ss.showtime.id = :showtimeId AND ss.seat.seatId = :seatId AND ss.status = 'held'")
    int forceReleaseSeat(@Param("showtimeId") Integer showtimeId,
                         @Param("seatId") Integer seatId);
}
