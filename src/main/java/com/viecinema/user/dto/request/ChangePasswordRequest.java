package com.viecinema.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.viecinema.common.constant.ValidationConstant.PASSWORD_MAX_LENGTH;
import static com.viecinema.common.constant.ValidationConstant.PASSWORD_MIN_LENGTH;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model for changing user password")
public class ChangePasswordRequest {

    @NotBlank(message = "Old password must not be empty")
    @Schema(description = "Current password", example = "OldPass123!")
    private String oldPassword;

    @NotBlank(message = "New password must not be empty")
    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH,
            message = "New password must be between 8 and 50 characters")
    @Schema(description = "New password", example = "NewPass123!")
    private String newPassword;

    @NotBlank(message = "Confirm password must not be empty")
    @Schema(description = "Confirmation of new password", example = "NewPass123!")
    private String confirmPassword;
}
