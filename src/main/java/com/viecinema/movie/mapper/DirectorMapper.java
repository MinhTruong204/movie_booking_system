package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.DirectorDto;
import com.viecinema.movie.entity.Director;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DirectorMapper {
    @Mapping(source = "id", target = "directorId")
    DirectorDto toDirectorDto(Director director);
}
