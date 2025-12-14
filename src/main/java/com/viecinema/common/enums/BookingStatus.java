package com.viecinema.common.enums;

public enum BookingStatus {
    PENDING("PENDING"),
    PAID("PAID"),
    CANCELLED("CANCELLED");

    private final String value;

    BookingStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
