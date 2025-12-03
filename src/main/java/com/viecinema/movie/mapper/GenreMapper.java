package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.GenreDto;
import com.viecinema.movie.entity.Genre;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    GenreDto toGenreDto(Genre genre);
}
