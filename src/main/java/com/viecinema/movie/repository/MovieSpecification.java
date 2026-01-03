package com.viecinema.movie.repository;

import com.viecinema.common.enums.MovieStatus;
import com.viecinema.movie.entity.Genre;
import com.viecinema.movie.entity.Movie;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class MovieSpecification {

    public static Specification<Movie> hasStatus(MovieStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Movie> hasGenres(List<Integer> genreIds) {
        return (root, query, criteriaBuilder) -> {
            if (genreIds == null || genreIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            Class<?> resultType = query.getResultType();
            boolean isCountQuery = (Long.class.equals(resultType) || long.class.equals(resultType));

            Join<Movie, Genre> genreJoin;

            if (isCountQuery) {
                genreJoin = root.join("genres", JoinType.INNER);
            } else {
                genreJoin = (Join<Movie, Genre>) root.getJoins().stream()
                        .filter(join -> "genres".equals(join.getAttribute().getName()))
                        .findFirst()
                        .orElseGet(() -> root.join("genres", JoinType.LEFT));
            }
            query.distinct(true);
            return genreJoin.get("genreId").in(genreIds);
        };
    }

    public static Specification<Movie> isNotDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    public static Specification<Movie> fetchGenres() {
        return (root, query, criteriaBuilder) -> {

            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("genres", JoinType.LEFT);
                query.distinct(true);
            }
            return criteriaBuilder.conjunction();
        };
    }
}
