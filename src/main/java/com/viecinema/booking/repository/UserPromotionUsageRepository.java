package com.viecinema.booking.repository;

import com.viecinema.booking.entity.UserPromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPromotionUsageRepository extends JpaRepository<UserPromotionUsage, Integer> {

    Optional<UserPromotionUsage> findByUserIdAndPromoId(Integer userId, Integer promoId);
}
