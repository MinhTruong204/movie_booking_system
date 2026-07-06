package com.viecinema.movie.mapper;

import com.viecinema.movie.dto.response.ReviewResponse;
import com.viecinema.movie.entity.MovieReview;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", injectionStrategy = InjectionStrategy.CONSTRUCTOR)
public interface ReviewMapper {
    @Mapping(source = "id",             target = "reviewId")
    @Mapping(source = "movie.movieId",  target = "movieId")
    @Mapping(source = "user.id",        target = "userId")
    @Mapping(source = "user.fullName",  target = "userFullName")
    @Mapping(target = "bonusPointsAwarded", ignore = true)
    ReviewResponse toReviewResponse(MovieReview review);
}
