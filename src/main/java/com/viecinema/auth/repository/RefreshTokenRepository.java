package com.viecinema.auth.repository;

import com.viecinema.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    // ============ Admin Session Management ============

    /**
     * Lấy tất cả sessions (refresh tokens) của user, sắp xếp theo thời gian sử dụng gần nhất
     */
    List<RefreshToken> findByUserIdOrderByLastUsedAtDesc(Integer userId);

    /**
     * Revoke toàn bộ refresh token của user.
     * Được gọi bởi SessionEventListener khi user bị ban/delete/reset password.
     */
    @Modifying
    @Transactional
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllByUserId(@Param("userId") Integer userId);

    /**
     * Xóa một session cụ thể của user
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshToken rt WHERE rt.user.id = :userId AND rt.id = :tokenId")
    void deleteByUserIdAndTokenId(@Param("userId") Integer userId, @Param("tokenId") Long tokenId);
}
