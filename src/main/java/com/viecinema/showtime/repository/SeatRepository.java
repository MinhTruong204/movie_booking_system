package com.viecinema.showtime.repository;

import com.viecinema.showtime.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SeatRepository extends JpaRepository<Seat,Integer> {
    @Query("""
        SELECT s FROM Seat s
        JOIN FETCH s.seatType st
        WHERE s.room.id = :roomId
        AND s.isActive = true
        AND s.deletedAt IS NULL
        ORDER BY s.seatRow ASC, s.seatNumber ASC
    """)
    List<Seat> findByRoomIdOrderBySeatRowAndNumber(@Param("roomId") Integer roomId);
}
