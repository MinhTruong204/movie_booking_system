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
                return criteriaBuilder.conjunction(); // WHERE 1=1 (không filter)
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<Movie> hasGenres(List<Integer> genreIds) {
        return (root, query, criteriaBuilder) -> {
            if (genreIds == null || genreIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            // JOIN với bảng genres (Many-to-Many)
            Class<?> resultType = query.getResultType();
            boolean isCountQuery = resultType != null &&
                    (Long.class.equals(resultType) || long.class.equals(resultType));

            Join<Movie, Genre> genreJoin;

            if (isCountQuery) {
                // COUNT QUERY: Dùng INNER JOIN thông thường (không fetch)
                genreJoin = root.join("genres", JoinType.INNER);
            } else {
                // DATA QUERY: Tìm fetch join đã tồn tại hoặc tạo LEFT JOIN
                // Tránh conflict với fetchGenres()
                genreJoin = (Join<Movie, Genre>) root. getJoins().stream()
                        .filter(join -> "genres".equals(join.getAttribute(). getName()))
                        .findFirst()
                        .orElseGet(() -> root.join("genres", JoinType.LEFT));
            }
            query.distinct(true);
            return genreJoin.get("genreId").in(genreIds);
        };
    }
    /**
     * Loại bỏ phim đã xóa (soft delete)
     *
     * SQL: WHERE deleted_at IS NULL
     */
    public static Specification<Movie> isNotDeleted() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isNull(root.get("deletedAt"));
    }

    /**
     * Eager fetch genres để tránh N+1 query problem
     *
     * Chỉ áp dụng khi query dữ liệu thực (không phải count query)
     */
    public static Specification<Movie> fetchGenres() {
        return (root, query, criteriaBuilder) -> {
            // Kiểm tra xem có phải count query không
            // Count query trả về Long/long, data query trả về Movie
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("genres", JoinType.LEFT);
                query.distinct(true);
            }
            return criteriaBuilder.conjunction();
        };
    }
}
