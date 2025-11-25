package com.viecinema.auth.dto.request;


import com.viecinema.common.constant.MessageConstant;
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
            message = MessageConstant.PASSWORD_LENGTH_ERROR)
    @Pattern(regexp = ValidationConstant.PASSWORD_REGEX,
            message = MessageConstant.PASSWORD_PATTERN_ERROR)
    private String password;

    @NotBlank(message = MessageConstant.CONFIRM_PASSWORD_REQUIRED)
    private String confirmPassword;

    @Past(message = MessageConstant.BIRTH_DATE_ERROR)
    private LocalDate birthDate;


    private Gender gender;

    @AssertTrue(message = MessageConstant.CONFIRM_PASSWORD_ERROR)
    public boolean isPasswordMatching() {
        if (password == null || confirmPassword == null) {
            return false;
        }
        return password.equals(confirmPassword);
    }

}
