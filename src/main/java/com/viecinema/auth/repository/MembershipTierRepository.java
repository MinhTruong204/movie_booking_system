package com.viecinema.auth.repository;

import com.viecinema.auth.user.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier,Integer> {
}
