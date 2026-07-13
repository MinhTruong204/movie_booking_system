package com.viecinema.loyalty.repository;

import com.viecinema.loyalty.entity.PointRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointRedemptionRepository extends JpaRepository<PointRedemption, Integer> {

    List<PointRedemption> findByUser_IdOrderByCreatedAtDesc(Integer userId);
}
