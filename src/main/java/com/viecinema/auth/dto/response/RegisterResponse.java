package com.viecinema.auth.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterResponse {
    @JsonProperty("email")
    private String email;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("message")
    @Builder.Default
    private String message ="";

    @JsonProperty("verification_required")
    @Builder.Default
    private Boolean verificationRequired = false;

    @JsonProperty("otp_sent_at")
    @Builder.Default
    private LocalDateTime otpSentAt = LocalDateTime.now();
}
