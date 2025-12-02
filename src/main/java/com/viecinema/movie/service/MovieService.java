package com.viecinema.movie.service;

import com.viecinema.common.enums.MovieStatus;
import com.viecinema.movie.dto.MovieFilterRequest;
import com.viecinema.movie.dto.MovieSummaryDto;
import com.viecinema.movie.dto.PagedResponse;
import com.viecinema.movie.entity.Movie;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

public interface MovieService {
    PagedResponse<MovieSummaryDto> getNowShowingMovies(MovieFilterRequest request);
}
