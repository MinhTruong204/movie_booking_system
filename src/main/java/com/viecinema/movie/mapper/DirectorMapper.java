package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.DirectorDto;
import com.viecinema.movie.entity.Director;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DirectorMapper {
    DirectorDto toDirectorDto(Director director);
}
