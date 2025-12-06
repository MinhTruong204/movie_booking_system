package com.viecinema.showtime.entity;

import com.viecinema.common.entity.DeletableEntity;
import com.viecinema.movie.entity.Movie;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "showtimes")
public class Showtime extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "showtime_id", nullable = false)
    private Integer id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movie;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @NotNull
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @ColumnDefault("1")
    @Column(name = "is_active")
    private Boolean isActive;
}