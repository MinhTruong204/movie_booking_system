package com.viecinema.showtime.repository;

import com.viecinema.showtime.dto.request.ShowtimeFilterRequest;
import com.viecinema.showtime.entity.Showtime;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class ShowtimeSpecifications {
    public static Specification<Showtime> hasMovieId(Integer movieId) {
        return (root, query, cb)
                -> movieId == null ? null : cb.equal(root.get("movie").get("id"), movieId);
    }
    public static Specification<Showtime> hasCinemaId(Integer cinemaId) {
        return (root, query, cb)
                -> cinemaId == null ? null : cb.equal(root.get("cinema").get("id"), cinemaId);
    }
    public static Specification<Showtime> hasRoomId(Integer roomId) {
        return (root, query, cb)
                -> roomId == null ? null : cb.equal(root.get("room").get("id"), roomId);
    }

    public static Specification<Showtime> hasDate(LocalDateTime date) {
        return (root, query, cb)
                -> date == null ? null : cb.between(root.get("startTime"), date, date.plusDays(1));
    }

    public static Specification<Showtime> hasCity(String city) {
        return (root, query, cb)
                -> (city == null || city.isEmpty()) ? null : cb.equal(root.get("cinema").get("city"), city);
    }

    public static Specification<Showtime> hasActiveOnly(boolean activeOnly) {
        return (root, query, cb)
                -> activeOnly ? cb.isTrue(root.get("isActive")) : null;
    }

    public static Specification<Showtime> hasFutureOnly(boolean futureOnly) {
        return (root, query, cb)
                -> futureOnly ? cb.greaterThanOrEqualTo(root.get("startTime"), LocalDateTime.now()) : null;
    }
}
