package com.viecinema.auth.repository;

import com.viecinema.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Integer> {

    Optional<EmailVerification> findByToken(String token);

    /**
     * Xóa các token chưa dùng của user (dùng khi resend)
     */
    @Modifying
    @Query("DELETE FROM EmailVerification ev WHERE ev.userId = :userId AND ev.isUsed = false")
    void deleteUnusedByUserId(@Param("userId") Integer userId);
}
