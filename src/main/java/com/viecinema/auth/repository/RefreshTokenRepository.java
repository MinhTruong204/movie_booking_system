package com.viecinema.auth.repository;

import com.viecinema.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query(value = """
    DELETE FROM refresh_tokens
    WHERE user_id = :userId
    AND id IN (
        SELECT id FROM (
            SELECT id FROM refresh_tokens
            WHERE user_id = :userId
            ORDER BY last_used_at DESC
            LIMIT 18446744073709551615 OFFSET 4
        ) t
    )
    """, nativeQuery = true)
    void deleteOldTokens(@Param("userId") Integer userId);
}
