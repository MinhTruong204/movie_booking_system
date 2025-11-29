package com.viecinema.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    private String accessToken;
    private String fullName;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn;
}
