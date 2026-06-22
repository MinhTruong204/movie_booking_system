package com.viecinema.admin.dto.request;

import com.viecinema.common.constant.ValidationConstant;
import com.viecinema.common.enums.Gender;
import com.viecinema.common.enums.Role;
import com.viecinema.common.validation.annotation.ValidEmail;
import com.viecinema.common.validation.annotation.ValidPhone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request tạo mới user bởi Admin.
 * Khác với RegisterRequest: không cần confirmPassword, có thể set role, auto verify email.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminCreateUserRequest {

    @NotBlank(message = "Full name is required")
    @Size(min = ValidationConstant.USERNAME_MIN_LENGTH,
            max = ValidationConstant.USERNAME_MAX_LENGTH)
    @Pattern(regexp = ValidationConstant.USERNAME_REGEX,
            message = "Full name can only contain letters and spaces")
    private String fullName;

    @NotBlank(message = "Email is required")
    @ValidEmail
    private String email;

    @ValidPhone
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = ValidationConstant.PASSWORD_MIN_LENGTH,
            max = ValidationConstant.PASSWORD_MAX_LENGTH,
            message = "Password must be between 8 and 50 characters long")
    @Pattern(regexp = ValidationConstant.PASSWORD_REGEX,
            message = "Password must contain uppercase, lowercase, numbers and special characters")
    private String password;

    private Role role = Role.CUSTOMER;

    private Gender gender;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    private Boolean isActive = true;
    private Boolean emailVerified = true;
}
