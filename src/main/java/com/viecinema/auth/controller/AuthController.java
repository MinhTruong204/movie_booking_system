package com.viecinema.auth.controller;

import com.viecinema.common.constant.ApiConstant;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.user.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiConstant.AUTH_PATH)
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(ApiConstant.REGISTER_PATH)
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ApiMessage.USER_CREATED, response,response.getFullName()));
    }

}
