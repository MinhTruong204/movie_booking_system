package com.viecinema.auth.repository;

import com.viecinema.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Integer> {
    Boolean existsByEmail(String email);
    Boolean existsByPhone(String phone);

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Boolean existsByEmailAndDeletedAtIsNull(String email);

    Boolean existsByPhoneAndDeletedAtIsNull(String phone);


    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    /**
     * Tìm user với eager loading membership tier
     * Tránh N+1 query problem
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.membershipTier " +
            "WHERE u.id = :userId AND u.deletedAt IS NULL")
    Optional<User> findByIdWithMembership(@Param("userId") Integer userId);

    /**
     * Tìm user active với membership
     */
    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.membershipTier " +
            "WHERE u.id = :userId " +
            "AND u.deletedAt IS NULL " +
            "AND u.isActive = true")
    Optional<User> findActiveUserWithMembership(@Param("userId") Integer userId);
}
