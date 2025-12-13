package com.viecinema.booking.repository;

import com.viecinema.booking.entity.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Integer> {

    Optional<Promotion> findByCodeAndIsActiveTrue(String code);

    @Modifying
    @Query("UPDATE Promotion p SET p.currentUsage = p.currentUsage + 1 WHERE p.id = :promoId")
    void incrementUsage(Integer promoId);
}