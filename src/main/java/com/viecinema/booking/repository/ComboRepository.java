package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboRepository extends JpaRepository<Combo, Integer> {
    List<Combo> findByIsActiveTrue();
    List<Combo> findByIdInAndIsActiveTrue(List<Integer> ids);
}
