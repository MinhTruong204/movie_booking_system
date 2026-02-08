package com.viecinema.movie.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GenreInfo(
        Integer genreId,
        String name,
        String description,
        Long movieCount
) {}
