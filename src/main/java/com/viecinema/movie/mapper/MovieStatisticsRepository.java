package com.viecinema.movie.mapper;

import com.viecinema.movie.entity.MovieStatistic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieStatisticsRepository extends JpaRepository<MovieStatistic,Integer> {
}
