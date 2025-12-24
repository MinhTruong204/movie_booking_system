package com.viecinema.common.exception.handler;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.booking.exception.SeatAlreadyHeldException;
import com.viecinema.booking.exception.SeatNotHeldByUserException;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.exception.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.RESOURCE_NOT_FOUND, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(SpecificBusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomBusinessException(SpecificBusinessException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.SPECIFIC_ERROR, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SeatAlreadyHeldException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomBusinessException(SeatAlreadyHeldException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.SPECIFIC_ERROR, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SeatNotHeldByUserException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomBusinessException(SeatNotHeldByUserException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.SPECIFIC_ERROR, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.FIELD_ALREADY_EXISTS, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(BadRequestException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.builder()
                .status(ApiMessage.VALIDATION_ERROR.getType())
                .statusCode(ApiMessage.VALIDATION_ERROR.getCode())
                .message(ex.getMessage())
                .build();
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        ApiMessage apiMessage = ex.getErrorCode();
        ApiResponse<Object> apiResponse = ApiResponse.error(apiMessage, ex.getMessage());
        HttpStatus status = apiMessage.getCode().startsWith("E409") ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(apiResponse, status);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.INVALID_CREDENTIALS);
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        // Log the exception for debugging purposes
        logger.error("An unexpected error occurred: ", ex);
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.SERVER_ERROR);
        return new ResponseEntity<>(apiResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.VALIDATION_ERROR);
        apiResponse.setData(errors);
        return new ResponseEntity<>(apiResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(LoginRequiredException.class)
    public ResponseEntity<ApiResponse<Object>> handleLoginRequiredException(LoginRequiredException ex) {
        ApiResponse<Object> apiResponse = ApiResponse.error(ApiMessage.SPECIFIC_ERROR, ex.getMessage());
        return new ResponseEntity<>(apiResponse, HttpStatus.UNAUTHORIZED);
    }
}