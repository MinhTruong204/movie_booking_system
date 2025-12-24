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
        HOLD_SEAT("S014", "success", "Hold seats successfully"),
        RELEASE_SEAT("S015", "success", "Release seats successfully"),

        // Delete operations

        // Retrieve operations
        RESOURCE_RETRIEVED("S033", "success", "%s retrieved successfully"),



        // ============================== ERROR MESSAGES  ==============================

        // Authentication & Authorization (HTTP 401, 403)
        INVALID_CREDENTIALS("E401", "error", "Invalid username or password"),

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
        CUSTOM_ERROR("E503", "error", "%s"),
        SPECIFIC_ERROR("E503", "error", "%s");

        private final String code;
        private final String type;
        private final String template;

        public String format(Object... args) {
            return String.format(getTemplate(), args);
        }
}