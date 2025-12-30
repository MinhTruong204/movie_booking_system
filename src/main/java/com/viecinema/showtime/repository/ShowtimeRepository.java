package com.viecinema.showtime.repository;

import com.viecinema.showtime.dto.projection.PricingSummary;
import com.viecinema.showtime.entity.Showtime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ShowtimeRepository extends JpaRepository<Showtime,Integer>,
                                            JpaSpecificationExecutor<Showtime> {
    @Query(value = """
        SELECT 
            st.name AS seat_type_name,
            (sh.base_price * st.price_multiplier) AS final_price,
            COUNT(CASE WHEN ss.status = 'available' THEN 1 END) AS available_count
        FROM showtimes sh
        INNER JOIN seat_status ss ON sh.showtime_id = ss.showtime_id
        INNER JOIN seats se ON ss.seat_id = se.seat_id
        INNER JOIN seat_types st ON se.seat_type_id = st.seat_type_id
        WHERE sh.showtime_id = :showtimeId
        GROUP BY st.seat_type_id, st.name, st.price_multiplier, sh.base_price
        ORDER BY final_price ASC
        """, nativeQuery = true)
    List<PricingSummary> findPricingInfoByShowtime(@Param("showtimeId") Integer showtimeId);

}
