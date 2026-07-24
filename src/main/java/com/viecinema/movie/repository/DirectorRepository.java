package com.viecinema.movie.repository;

import com.viecinema.movie.entity.Director;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectorRepository extends JpaRepository<Director, Integer> {

    /**
     * Lấy danh sách Director theo danh sách ID.
     * Dùng khi tạo/cập nhật phim để resolve directorIds → Director entities.
     */
    List<Director> findAllByIdIn(List<Integer> ids);
}
