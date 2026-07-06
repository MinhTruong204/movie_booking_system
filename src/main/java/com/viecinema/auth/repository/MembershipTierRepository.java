package com.viecinema.auth.repository;

import com.viecinema.auth.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {

    @Query("SELECT mt FROM MembershipTier mt " +
            "WHERE mt.pointsRequired > :currentPoints " +
            "ORDER BY mt.pointsRequired ASC")
    List<MembershipTier> findNextTier(@Param("currentPoints") Integer currentPoints);

    /**
     * Tìm hạng cao nhất mà user đủ điều kiện (pointsRequired <= currentPoints).
     * Dùng để tự động nâng hạng sau khi cộng điểm.
     */
    @Query("SELECT mt FROM MembershipTier mt " +
            "WHERE mt.pointsRequired <= :currentPoints " +
            "ORDER BY mt.pointsRequired DESC " +
            "LIMIT 1")
    Optional<MembershipTier> findTopEligibleTier(@Param("currentPoints") Integer currentPoints);
}
