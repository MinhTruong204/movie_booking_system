package com.viecinema.auth.dto.request;


import com.viecinema.common.constant.ValidationConstant;
import com.viecinema.common.enums.Gender;
import com.viecinema.common.validation.annotation.ValidEmail;
import com.viecinema.common.validation.annotation.ValidPhone;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank
    @Size(min = ValidationConstant.USERNAME_MIN_LENGTH,
            max = ValidationConstant.USERNAME_MAX_LENGTH)
    @Pattern(regexp = ValidationConstant.USERNAME_REGEX)
    private String fullName;

    @NotBlank
    @ValidEmail
    private String email;

    @ValidPhone
    private String phone;

    @NotBlank
    @Size(min = ValidationConstant.PASSWORD_MIN_LENGTH,
            max = ValidationConstant.PASSWORD_MAX_LENGTH,
            message = "Password must be between 8 and 50 characters long")
    @Pattern(regexp = ValidationConstant.PASSWORD_REGEX,
            message = "Password must contain uppercase, lowercase, numbers and special characters")
    private String password;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;


    private Gender gender;

    @AssertTrue(message = "Password and confirm password must match")
    public boolean isPasswordMatching() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

}
