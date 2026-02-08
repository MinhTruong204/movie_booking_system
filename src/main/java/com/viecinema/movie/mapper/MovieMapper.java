package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.MovieDetail;
import com.viecinema.movie.dto.MovieSummary;
import com.viecinema.movie.entity.Movie;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring",
        uses = {ActorMapper.class, DirectorMapper.class, GenreMapper.class},
        injectionStrategy = InjectionStrategy.CONSTRUCTOR) // Define this interface as a Spring bean
public interface MovieMapper {
    MovieSummary toMovieSummaryDto(Movie movie);

    MovieDetail toMovieDetailDto(Movie movie);
}
