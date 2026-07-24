package com.viecinema.movie.repository;

import com.viecinema.movie.entity.Actor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActorRepository extends JpaRepository<Actor, Integer> {

    /**
     * Lấy danh sách Actor theo danh sách ID.
     * Dùng khi tạo/cập nhật phim để resolve actorIds → Actor entities.
     */
    List<Actor> findAllByIdIn(List<Integer> ids);
}
