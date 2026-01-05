package com.viecinema.showtime.repository;

import com.viecinema.common.enums.SeatStatusType;
import com.viecinema.showtime.dto.projection.SeatStatusCount;
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

@Repository
public interface SeatStatusRepository extends JpaRepository<SeatStatus, Integer> {

    @Query("""
                SELECT ss FROM SeatStatus ss
                JOIN FETCH ss.seat s
                JOIN FETCH s.seatType st
                LEFT JOIN FETCH ss.heldByUser u
                WHERE ss.showtime.id = :showtimeId
            """)
    List<SeatStatus> findByShowtimeId(@Param("showtimeId") Integer showtimeId);

    @Query("""
                SELECT ss.status AS status, COUNT(ss) AS count
                FROM SeatStatus ss
                WHERE ss.showtime.id = :showtimeId
                GROUP BY ss.status
            """)
    List<SeatStatusCount> countByShowtimeIdGroupByStatus(@Param("showtimeId") Integer showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ss FROM SeatStatus ss WHERE ss.showtime.id = :showtimeId AND ss.seat.seatId IN :seatIds")
    List<SeatStatus> findByShowtimeAndSeatsWithLock(
            @Param("showtimeId") Integer showtimeId,
            @Param("seatIds") List<Integer> seatIds);

    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = 'available', ss.heldByUser.id = NULL, " +
            "ss.heldUntil = NULL WHERE ss.heldByUser.id = :userId AND ss.status = 'held'")
    int releaseUserSeats(@Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = 'available', ss.heldByUser.id = NULL, ss.heldUntil = NULL, ss.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ss.showtime.id = :showtimeId AND ss.seat.seatId = :seatId AND ss.status = 'held' AND ss.heldByUser.id = :userId")
    int releaseSeatByUser(@Param("showtimeId") Integer showtimeId,
                          @Param("seatId") Integer seatId,
                          @Param("userId") Integer userId);

    @Modifying
    @Query("UPDATE SeatStatus ss SET ss.status = 'available', ss.heldByUser.id = NULL, ss.heldUntil = NULL, ss.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE ss.showtime.id = :showtimeId AND ss.seat.seatId = :seatId AND ss.status = 'held'")
    int forceReleaseSeat(@Param("showtimeId") Integer showtimeId,
                         @Param("seatId") Integer seatId);

    List<SeatStatus> findAllByStatusAndCreatedAtBefore(SeatStatusType status, LocalDateTime createdAtBefore);
}
