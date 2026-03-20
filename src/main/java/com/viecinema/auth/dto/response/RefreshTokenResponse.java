package com.viecinema.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class RefreshTokenResponse {
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
