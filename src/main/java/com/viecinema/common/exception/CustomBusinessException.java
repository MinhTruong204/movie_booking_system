package com.viecinema.common.exception;

public class CustomBusinessException extends RuntimeException {

    // Constructor nhận một thông báo duy nhất
    public CustomBusinessException(String message) {
        super(message);
    }

    // Constructor nhận cả thông báo và nguyên nhân gốc (cause)
    public CustomBusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
