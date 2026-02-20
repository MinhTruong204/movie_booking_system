package com.viecinema.auth.controller;

import com.viecinema.auth.dto.request.LoginRequest;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.dto.response.LoginResponse;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.service.AuthService;
import com.viecinema.common.constant.ApiConstant;
import com.viecinema.common.constant.ApiMessage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000", allowCredentials = "true")
@RestController
@RequestMapping(ApiConstant.AUTH_PATH)
@AllArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new customer account. Returns the created user details."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error – invalid input fields"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email or phone number already exists")
    })
    @SecurityRequirements   // No auth required for this endpoint
    @PostMapping(ApiConstant.REGISTER_PATH)
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ApiMessage.RESOURCE_CREATE, registerResponse,
                        "User " + registerResponse.getFullName()));
    }

    @Operation(
            summary = "Login",
            description = "Authenticates a user with email and password. Returns a JWT access token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login successful – JWT token returned",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid email or password")
    })
    @SecurityRequirements   // No auth required for this endpoint
    @PostMapping(ApiConstant.LOGIN_PATH)
    public ResponseEntity<ApiResponse> login(@Valid @RequestBody LoginRequest loginrequest) {
        LoginResponse loginResponse = authService.login(loginrequest);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, loginResponse, "User"));
    }

}


