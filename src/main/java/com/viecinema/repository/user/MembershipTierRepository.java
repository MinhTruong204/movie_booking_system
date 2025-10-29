package com.viecinema.repository.user;

import com.viecinema.entity.user.MembershipTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MembershipTierRepository extends JpaRepository<MembershipTier,Integer> {
}
