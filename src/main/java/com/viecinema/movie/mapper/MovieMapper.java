package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.MovieDetailDto;
import com.viecinema.movie.dto.MovieSummaryDto;
import com.viecinema.movie.entity.Movie;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",
        uses = {ActorMapper.class, DirectorMapper.class, GenreMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR) // Define this interface as a Spring bean
public interface MovieMapper {
    MovieSummaryDto toMovieSummaryDto(Movie movie);

    MovieDetailDto toMovieDetailDto(Movie movie);
}
