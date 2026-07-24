package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.MovieDetail;
import com.viecinema.movie.dto.MovieSummary;
import com.viecinema.movie.dto.response.AdminMovieResponse;
import com.viecinema.movie.entity.Movie;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring",
        uses = {ActorMapper.class, DirectorMapper.class, GenreMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR) // Define this interface as a Spring bean
public interface MovieMapper {
    MovieSummary toMovieSummaryDto(Movie movie);

    MovieDetail toMovieDetailDto(Movie movie);

    /**
     * Map Movie → AdminMovieResponse.
     * status được lấy từ enum MovieStatus → String (tên enum).
     */
    @Mapping(target = "status", expression = "java(movie.getStatus() != null ? movie.getStatus().name() : null)")
    AdminMovieResponse toAdminMovieResponse(Movie movie);
}

