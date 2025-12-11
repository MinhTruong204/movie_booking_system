package com.viecinema.auth.dto.response;


import com.viecinema.common.constant.ApiMessage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiResponse<T> {

    private String status;
    private String statusCode;
    private String message;
    private LocalDateTime timestamp;
    private T data;

    public static <T> ApiResponse<T> success(ApiMessage apiMessage, T data, Object... args) {
        String messageDetail = apiMessage.format(args);
        return ApiResponse.<T>builder()
                .status(apiMessage.getType())
                .statusCode(apiMessage.getCode())
                .message(messageDetail)
                .timestamp(LocalDateTime.now())
                .data(data)
                .build();
    }
    public static <T> ApiResponse<T> successWithoutData(ApiMessage apiMessage, Object... args) {
        String messageDetail = apiMessage.format(args);
        return ApiResponse.<T>builder()
                .status(apiMessage.getType())
                .statusCode(apiMessage.getCode())
                .message(messageDetail)
                .timestamp(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(ApiMessage apiMessage, Object... args) {
        String messageDetail = apiMessage.format(args);
        return ApiResponse.<T>builder()
                .status(apiMessage.getType())
                .statusCode(apiMessage.getCode())
                .message(messageDetail)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
