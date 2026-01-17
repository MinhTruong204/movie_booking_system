package com.viecinema.auth.controller;

import com.viecinema.auth.dto.request.LoginRequest;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.dto.response.LoginResponse;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.service.AuthService;
import com.viecinema.common.constant.ApiConstant;
import com.viecinema.common.constant.ApiMessage;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping(ApiConstant.AUTH_PATH)
@AllArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping(ApiConstant.REGISTER_PATH)
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ApiMessage.RESOURCE_CREATE, registerResponse,
                        "User " + registerResponse.getFullName()));
    }

    @PostMapping(ApiConstant.LOGIN_PATH)
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginrequest) {
        LoginResponse loginResponse = authService.login(loginrequest);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, loginResponse, "User"));
    }

}
