package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.MovieSummaryDto;
import com.viecinema.movie.entity.Movie;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring") // Define this interface as a Spring bean
public interface MovieMapper {
    MovieSummaryDto toMovieSummaryDto (Movie movie);
}
