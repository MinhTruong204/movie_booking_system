package com.viecinema.movie.dto;

public interface GenreFullProjection {
    Integer getGenreId();
    String getName();
    String getDescription();
    Long getMovieCount();
}
