package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.ActorDto;
import com.viecinema.movie.entity.Actor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActorMapper {
    ActorDto toActorDto(Actor actor);
}
