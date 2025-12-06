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
        ORDER BY s. seatRow ASC, s.seatNumber ASC
    """)
    List<Seat> findByRoomIdOrderBySeatRowAndNumber(@Param("roomId") Integer roomId);

    /**
     * Đếm số ghế theo loại trong một phòng
     */
    @Query("""
        SELECT s.seatType.seatTypeId, COUNT(s)
        FROM Seat s
        WHERE s.room.id = :roomId
        AND s.isActive = true
        AND s.deletedAt IS NULL
        GROUP BY s. seatType.seatTypeId
    """)
    List<Object[]> countSeatsByTypeInRoom(@Param("roomId") Integer roomId);

    /**
     * Lấy thông tin layout phòng (số hàng, max ghế/hàng)
     */
    @Query("""
        SELECT 
            COUNT(DISTINCT s.seatRow) as totalRows,
            MAX(s.seatNumber) as maxSeatsPerRow
        FROM Seat s
        WHERE s.room.id = :roomId
        AND s.isActive = true
        AND s.deletedAt IS NULL
    """)
    Object[] getRoomLayoutInfo(@Param("roomId") Integer roomId);
}
