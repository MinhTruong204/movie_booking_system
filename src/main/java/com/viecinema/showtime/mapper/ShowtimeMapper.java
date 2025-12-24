package com.viecinema.showtime.mapper;

import com.viecinema.movie.entity.Genre;
import com.viecinema.movie.entity.Movie;
import com.viecinema.showtime.dto.CinemaInfo;
import com.viecinema.showtime.dto.MovieInfo;
import com.viecinema.showtime.dto.RoomInfo;
import com.viecinema.showtime.dto.SeatAvailability;
import com.viecinema.showtime.dto.response.ShowtimeDetailResponse;
import com.viecinema.showtime.entity.Cinema;
import com.viecinema.showtime.entity.Room;
import com.viecinema.showtime.entity.Showtime;
import org.mapstruct.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring",
        injectionStrategy = InjectionStrategy.CONSTRUCTOR) // Define this interface as a Spring bean
public interface ShowtimeMapper {
    @Mapping(target = "showtimeId", source = "id")
    @Mapping(target = "cinema", source = "room.cinema")
    @Mapping(target = "timeSlot", ignore = true)
    @Mapping(target = "movie", source = "movie")
    @Mapping(target = "room", source = "room")
    @Mapping(target = "seatAvailability", ignore = true)
    @Mapping(target = "pricing", ignore = true)
    @Mapping(target = "format", ignore = true)
    ShowtimeDetailResponse toResponse(Showtime dto);

    List<ShowtimeDetailResponse> toResponseList(List<Showtime> showtimes);

    MovieInfo toMovieInfo(Movie movie);
    @Mapping(source = "id", target = "cinemaId")
    CinemaInfo toCinemaInfo(Cinema cinema);
    @Mapping(source = "id", target = "roomId")
    RoomInfo toRoomInfo(Room room);

    default List<String> mapGenres(Set<Genre> genres) {
        if (genres == null) return Collections.emptyList();
        return genres.stream().map(Genre::getName).toList();
    }

    @AfterMapping
    default void calculateFields(@MappingTarget ShowtimeDetailResponse response) {
        response.setTimeSlot(response.calculateTimeSlot());
    }
}
