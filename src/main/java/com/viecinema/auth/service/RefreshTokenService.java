package com.viecinema.auth.service;

import com.viecinema.auth.dto.response.RefreshTokenResponse;
import com.viecinema.auth.entity.RefreshToken;
import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.RefreshTokenRepository;
import com.viecinema.common.enums.TokenType;
import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.ResourceNotFoundException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final EntityManager entityManager;

    @Transactional
    public RefreshTokenResponse refreshToken(String refreshToken, String ipAddress, String userAgent) {
        if(!jwtService.validateToken(refreshToken, TokenType.REFRESH))
            throw new BadRequestException("Invalid refresh token");

        // Find in database
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new ResourceNotFoundException("Refresh token"));

        // Get info
        String email = jwtService.extractUsername(refreshToken, TokenType.REFRESH);
        Integer userId = refreshTokenEntity.getUser().getId();

        String newRefreshToken =  createAndSaveRefreshToken(userId, email, ipAddress, userAgent);
        String newAccessToken = jwtService.generateAccessToken(email,null);

        refreshTokenRepository.delete(refreshTokenEntity);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getExpirationInSeconds(newAccessToken,TokenType.ACCESS))
                .build();
    }

    @Transactional
    public String createAndSaveRefreshToken(Integer userId, String email, String ipAddress, String userAgent) {

        // Create string refresh token
        String refreshToken = jwtService.generateRefreshToken(email,null);
        User userProxy = entityManager.getReference(User.class, userId);
        // Create refresh token entity
        RefreshToken refreshTokenEntity = RefreshToken.builder()
                .user(userProxy)
                .token(refreshToken)
                .expiryDate(jwtService.extractExpiration(refreshToken,TokenType.REFRESH).toInstant())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();

        // Check database
        refreshTokenRepository.deleteOldTokens(userId);
        // Save to database
        refreshTokenRepository.save(refreshTokenEntity);
        return refreshToken;
    }
}
