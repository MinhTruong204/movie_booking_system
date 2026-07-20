package com.viecinema.auth.dto.request;

import com.viecinema.common.validation.annotation.ValidEmail;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model for requesting temporary password via email")
public class ForgotPasswordRequest {

    @NotBlank(message = "Email must not be empty")
    @ValidEmail
    @Schema(description = "Registered email address", example = "user@example.com")
    private String email;
}
