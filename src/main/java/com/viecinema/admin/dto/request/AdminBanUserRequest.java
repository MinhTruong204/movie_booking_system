package com.viecinema.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request khóa tài khoản user bởi Admin.
 * lockDurationHours = null nghĩa là khóa vĩnh viễn (cho đến khi admin unban).
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminBanUserRequest {

    @NotBlank(message = "Ban reason is required")
    private String reason;

    /**
     * Thời gian khóa (giờ). Null = khóa vĩnh viễn.
     * Ví dụ: 24 = khóa 24 giờ, 720 = khóa 30 ngày.
     */
    private Integer lockDurationHours;
}
