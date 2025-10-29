package com.viecinema.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ApiMessage {
        // ========== SUCCESS MESSAGES ==========

        // Create operations
        USER_CREATED("S001", "success", "User '%s' created successfully"),
        USER_PROFILE_CREATED("S002", "success", "Profile for user '%s' created successfully"),
        ORDER_CREATED("S003", "success", "Order #%s created successfully with total $%.2f"),

        // Update operations
        USER_UPDATED("S011", "success", "User '%s' updated successfully"),
        USER_PASSWORD_CHANGED("S012", "success", "Password changed successfully for user '%s'"),
        USER_EMAIL_UPDATED("S013", "success", "Email updated to '%s' successfully"),

        // Delete operations
        USER_DELETED("S021", "success", "User '%s' deleted successfully"),
        ORDER_CANCELLED("S022", "success", "Order #%s cancelled successfully"),

        // Retrieve operations
        USER_RETRIEVED("S031", "success", "User profile retrieved successfully"),
        USERS_RETRIEVED("S032", "success", "%d users retrieved successfully"),
        ORDER_DETAILS_RETRIEVED("S033", "success", "Order #%s details retrieved successfully"),

        // ========== ERROR MESSAGES  ==========

        // Authentication & Authorization (HTTP 401, 403)
        INVALID_CREDENTIALS("E401", "error", "Invalid username or password"),
        SESSION_EXPIRED("E401_EXPIRED", "error", "Session expired. Please login again."),
        UNAUTHORIZED_ACCESS("E403", "error", "You don't have permission to access this resource"),
        ACCOUNT_LOCKED("E403_LOCKED", "error", "Account is locked. Contact support."),

        // Not Found (HTTP 404)
        RESOURCE_NOT_FOUND("E404", "error", "Resource not found"),
        USER_NOT_FOUND("E404_USER", "error", "User not found"),
        ORDER_NOT_FOUND("E404_ORDER", "error", "Order not found"),

        // Conflict (HTTP 409)
        FIELD_ALREADY_EXISTS("E409_FIELD", "error", "%s already exists"),
        DUPLICATE_EMAIL("E409_EMAIL", "error", "An account with this email already exists"),
        DUPLICATE_PHONE("E409_PHONE", "error", "An account with this phone number already exists"),
        INSUFFICIENT_STOCK("E409_STOCK", "error", "Insufficient stock for product '%s'"),
        ORDER_ALREADY_CANCELLED("E409_CANCEL", "error", "This order has already been cancelled"),

        // Validation & Bad Request (HTTP 400)
        REGISTRATION_FAILED("E400_REG", "error", "Registration failed due to invalid data."),
        VALIDATION_ERROR("E400_VALID", "error", "Validation failed"),
        FIELD_REQUIRED("E400_REQ", "error", "%s is required"),
        FIELD_INVALID("E400_INV", "error", "%s is invalid"),
        FIELD_SIZE("E400_SIZE", "error", "%s must be between %d and %d characters long"),
        FIELD_TOO_LONG("E400_LONG", "error", "%s cannot be longer than %d characters"),
        FIELD_TOO_SHORT("E400_SHORT", "error", "%s must be at least %d characters long"),
        FIELD_OUT_OF_RANGE("E400_RANGE", "error", "%s must be between %d and %d"),
        FIELD_PATTERN_MISMATCH("E400_PAT", "error", "%s does not match the required pattern"),
        PASSWORD_ERROR("E400_PASS", "error", "Password must contain uppercase, lowercase, numbers and special characters"),
        PASSWORD_MISMATCH("E400_MIS", "error", "Passwords do not match"),

        // Server Errors (HTTP 5xx)
        SERVER_ERROR("E500", "error", "An unexpected error occurred. Please try again later."),
        DATABASE_ERROR("E500_DB", "error", "Unable to process request. Please try again."),
        SERVICE_UNAVAILABLE("E503", "error", "Service temporarily unavailable");

        private final String code;
        private final String type;
        private final String template;

        public String format(Object... args) {
            return String.format(getTemplate(), args);
        }
}