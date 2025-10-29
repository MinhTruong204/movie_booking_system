package com.viecinema.exception;

import com.viecinema.constant.ApiMessage;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ApiMessage errorCode;
    private final String customMessage;

    public BusinessException(ApiMessage errorCode) {
        super(errorCode.getTemplate());
        this.errorCode = errorCode;
        this.customMessage = null;
    }

    public BusinessException(ApiMessage errorCode, String message) {
        this.errorCode = errorCode;
        this.customMessage = message;
    }

    @Override
    public String getMessage() {
        return customMessage != null ? customMessage : errorCode.getTemplate();
    }
}
