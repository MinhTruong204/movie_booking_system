package com.viecinema.common.enums;

public enum SeatStatusType {
    AVAILABLE("AVAILABLE"),
    BOOKED("BOOKED"),
    HELD("HELD");

    private final String value;

    SeatStatusType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
