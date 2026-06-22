package com.viecinema.common.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ApiMessage {
    // ============================== SUCCESS MESSAGES ==============================

    // Create operations
    RESOURCE_CREATE("S004", "success", "%s created successfully"),

    // Update operations
    RESOURCE_UPDATED("S005", "success", "%s updated successfully"),
    HOLD_SEAT("S014", "success", "Hold seats successfully"),
    RELEASE_SEAT("S015", "success", "Release seats successfully"),

    // Delete operations
    RESOURCE_DELETED("S006", "success", "%s deleted successfully"),
    RESOURCE_RESTORED("S007", "success", "%s restored successfully"),

    // Admin operations
    USER_BANNED("S008", "success", "User banned successfully"),
    USER_UNBANNED("S009", "success", "User unbanned successfully"),
    PASSWORD_RESET_SUCCESS("S010", "success", "Password reset successfully"),
    ROLE_CHANGED("S011", "success", "Role changed successfully"),
    IMPORT_COMPLETED("S012", "success", "Import completed: %s success, %s failed"),
    SESSIONS_REVOKED("S013", "success", "Sessions revoked successfully"),

    // Retrieve operations
    RESOURCE_RETRIEVED("S033", "success", "%s retrieved successfully"),


    // ============================== ERROR MESSAGES  ==============================

    // Authentication & Authorization (HTTP 401, 403)
    INVALID_CREDENTIALS("E401", "error", "Invalid username or password"),
    FORBIDDEN("E403", "error", "You don't have permission to perform this action"),

    // Not Found (HTTP 404)
    RESOURCE_NOT_FOUND("E404", "error", "Resource not found"),

    // Conflict (HTTP 409)
    FIELD_ALREADY_EXISTS("E409_FIELD", "error", "%s already exists"),
    DUPLICATE_EMAIL("E409_EMAIL", "error", "An account with this email already exists"),
    DUPLICATE_PHONE("E409_PHONE", "error", "An account with this phone number already exists"),

    // Validation & Bad Request (HTTP 400)
    VALIDATION_ERROR("E400_VALID", "error", "Validation failed"),
    FIELD_INVALID("E400_INV", "error", "%s is invalid"),
    FIELD_TOO_LONG("E400_LONG", "error", "%s cannot be longer than %d characters"),

    // Server Errors (HTTP 5xx)
    SERVER_ERROR("E500", "error", "An unexpected error occurred. Please try again later."),

    // Other
    SPECIFIC_ERROR("E503", "error", "%s");

    private final String code;
    private final String type;
    private final String template;

    public String format(Object... args) {
        return String.format(getTemplate(), args);
    }
}