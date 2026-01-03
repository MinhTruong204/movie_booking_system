package com.viecinema.auth.dto.request;

import com.viecinema.common.validation.annotation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import static com.viecinema.common.constant.ValidationConstant.PASSWORD_MAX_LENGTH;
import static com.viecinema.common.constant.ValidationConstant.PASSWORD_MIN_LENGTH;

@Data
public class LoginRequest {
    @NotBlank(message = "Email must not be empty")
    @ValidEmail
    String email;
    @NotBlank(message = "Password must not be empty")
    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH,
            message = "Password must be between 8 and 50 characters")
    String password;
}
