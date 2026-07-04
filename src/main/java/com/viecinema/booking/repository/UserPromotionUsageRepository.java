package com.viecinema.booking.repository;

import com.viecinema.booking.entity.UserPromotionUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPromotionUsageRepository extends JpaRepository<UserPromotionUsage, Integer> {

    @Query("SELECT u FROM UserPromotionUsage u WHERE u.user.id = :userId AND u.promo.id = :promoId")
    Optional<UserPromotionUsage> findByUserIdAndPromoId(
            @Param("userId") Integer userId,
            @Param("promoId") Integer promoId);
}
