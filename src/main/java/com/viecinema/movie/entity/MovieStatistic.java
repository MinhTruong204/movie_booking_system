package com.viecinema.movie.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "movie_statistics")
public class MovieStatistic {
    @Id
    @Column(name = "movie_id", nullable = false)
    private Integer id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "movie_id", nullable = false)
    private Movie movies;

    @ColumnDefault("0")
    @Column(name = "total_bookings")
    private Integer totalBookings;

    @ColumnDefault("0.00")
    @Column(name = "total_revenue", precision = 15, scale = 2)
    private BigDecimal totalRevenue;

    @ColumnDefault("0.00")
    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @ColumnDefault("0")
    @Column(name = "total_reviews")
    private Integer totalReviews;

    @ColumnDefault("0")
    @Column(name = "total_seats_sold")
    private Integer totalSeatsSold;

    @ColumnDefault("0.00")
    @Column(name = "occupancy_rate", precision = 5, scale = 2)
    private BigDecimal occupancyRate;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "last_updated")
    private Instant lastUpdated;

}