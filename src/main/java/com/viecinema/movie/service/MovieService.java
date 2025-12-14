package com.viecinema.movie.service;

import com.viecinema.common.enums.MovieStatus;
import com.viecinema.movie.dto.MovieDetailDto;
import com.viecinema.movie.dto.request.MovieFilterRequest;
import com.viecinema.movie.dto.MovieSummaryDto;
import com.viecinema.movie.dto.response.PagedResponse;

public interface MovieService {
    PagedResponse<MovieSummaryDto> getMoviesByStatus(MovieFilterRequest request, MovieStatus status);
    MovieDetailDto getMovieDetail(Integer movieId);
}
