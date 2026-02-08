package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.GenreBasicProjection;
import com.viecinema.movie.dto.GenreFullProjection;
import com.viecinema.movie.dto.GenreInfo;
import com.viecinema.movie.entity.Genre;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface GenreMapper {
    GenreInfo toGenreDto(GenreBasicProjection genre);
    GenreInfo toGenreDto(GenreFullProjection genre);
    GenreInfo toGenreDto(Genre genre);
}
