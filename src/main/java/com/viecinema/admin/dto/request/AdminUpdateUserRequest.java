package com.viecinema.admin.dto.request;

import com.viecinema.common.constant.ValidationConstant;
import com.viecinema.common.enums.Gender;
import com.viecinema.common.validation.annotation.ValidEmail;
import com.viecinema.common.validation.annotation.ValidPhone;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Request cập nhật thông tin user bởi Admin.
 * Tất cả fields đều optional (partial update).
 * Không cho phép đổi role/password qua endpoint này (có endpoint riêng).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUpdateUserRequest {

    @Size(min = ValidationConstant.USERNAME_MIN_LENGTH,
            max = ValidationConstant.USERNAME_MAX_LENGTH)
    @Pattern(regexp = ValidationConstant.USERNAME_REGEX,
            message = "Full name can only contain letters and spaces")
    private String fullName;

    @ValidEmail
    private String email;

    @ValidPhone
    private String phone;

    private Gender gender;

    @Past(message = "Birth date must be in the past")
    private LocalDate birthDate;

    private Boolean isActive;

    private Boolean emailVerified;

    private Boolean phoneVerified;
}
