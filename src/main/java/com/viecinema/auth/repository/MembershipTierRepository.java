package com.viecinema.auth.repository;

import com.viecinema.auth.entity.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier, Integer> {
    @Query("SELECT mt FROM MembershipTier mt " +
            "WHERE mt.pointsRequired > :currentPoints " +
            "ORDER BY mt.pointsRequired ASC")
    List<MembershipTier> findNextTier(@Param("currentPoints") Integer currentPoints);
}
