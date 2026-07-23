package com.viecinema.showtime.repository;

import com.viecinema.showtime.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Integer> {
    Optional<Room> findByIdAndDeletedAtIsNull(Integer id);
}
