package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.ActorInfo;
import com.viecinema.movie.entity.Actor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ActorMapper {
    ActorInfo toActorDto(Actor actor);
}
