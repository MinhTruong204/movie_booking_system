package com.viecinema.admin.dto.request;

import com.viecinema.common.constant.ValidationConstant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request reset password user bởi Admin.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminResetPasswordRequest {

    @NotBlank(message = "New password is required")
    @Size(min = ValidationConstant.PASSWORD_MIN_LENGTH,
            max = ValidationConstant.PASSWORD_MAX_LENGTH,
            message = "Password must be between 8 and 50 characters long")
    @Pattern(regexp = ValidationConstant.PASSWORD_REGEX,
            message = "Password must contain uppercase, lowercase, numbers and special characters")
    private String newPassword;
}
