package com.viecinema.loyalty.repository;

import com.viecinema.loyalty.entity.LoyaltyPointsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface LoyaltyPointsConfigRepository extends JpaRepository<LoyaltyPointsConfig, Integer> {

    Optional<LoyaltyPointsConfig> findByConfigKey(String configKey);
}
