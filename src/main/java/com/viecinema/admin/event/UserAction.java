package com.viecinema.admin.event;

/**
 * Enum định nghĩa các loại hành động admin thực hiện trên User.
 * Được sử dụng trong Event System (Observer Pattern) để phân loại sự kiện.
 */
public enum UserAction {
    CREATED,
    UPDATED,
    BANNED,
    ACTIVATED,
    SOFT_DELETED,
    RESTORED,
    PASSWORD_RESET,
    ROLE_CHANGED
}
