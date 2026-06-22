package com.viecinema.admin.dto.request;

import com.viecinema.common.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request đổi role user bởi Admin.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminChangeRoleRequest {

    @NotNull(message = "Role is required")
    private Role role;

    private String reason;
}
