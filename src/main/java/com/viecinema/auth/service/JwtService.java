package com.viecinema.auth.service;

import com.viecinema.common.enums.TokenType;
import com.viecinema.config.AuthConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final AuthConfig authConfig;

    //  Create a secret key to encode using sha
    private SecretKey getSigninKey(TokenType tokenType) {
        // Convert secret key to byte
        byte[] secretKeyByte = (tokenType == TokenType.ACCESS)
                ? authConfig.getAccessTokenSecret().getBytes()
                : authConfig.getRefreshTokenSecret().getBytes();
        return Keys.hmacShaKeyFor(secretKeyByte);
    }

    public String generateAccessToken(String email, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "access");
        if (extraClaims != null)
            claims.putAll(extraClaims);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + authConfig.getAccessTokenExpire().toMillis()))
                .signWith(getSigninKey(TokenType.ACCESS))
                .compact();
    }

    public String generateRefreshToken(String email, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", "refresh");
        if (extraClaims != null)
            claims.putAll(extraClaims);
        return Jwts.builder()
                .subject(email)
                .claims(claims)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + authConfig.getRefreshTokenExpire().toMillis()))
                .signWith(getSigninKey(TokenType.REFRESH))
                .compact();
    }

    public String extractUsername(String token, TokenType tokenType) {
        return extractClaim(token,tokenType, Claims::getSubject);
    }

    public Date extractExpiration(String token, TokenType tokenType) {
        return extractClaim(token,tokenType, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, TokenType tokenType, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token,tokenType);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token, TokenType tokenType) {
        return Jwts.parser()
                .verifyWith(getSigninKey(tokenType))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token, TokenType tokenType) {
        return extractExpiration(token,tokenType).before(new Date());
    }

    public Boolean validateToken(String token, TokenType tokenType) {
        return  !isTokenExpired(token,tokenType);
    }

    public long getExpirationInSeconds(String token, TokenType tokenType) {
        Date expiration = extractClaim(token,tokenType, Claims::getExpiration);
        long diff = expiration.getTime() - System.currentTimeMillis();
        return Math.max(0, diff / 1000);
    }

}
