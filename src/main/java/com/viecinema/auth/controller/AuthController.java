package com.viecinema.auth.controller;

import com.viecinema.auth.dto.request.LoginRequest;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.response.LoginResponse;
import com.viecinema.auth.dto.response.RefreshTokenResponse;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.service.AuthService;
import com.viecinema.auth.service.RefreshTokenService;
import com.viecinema.common.constant.ApiConstant;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.common.util.RequestUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiConstant.AUTH_PATH)
@AllArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new customer account. Returns the created user details."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error – invalid input fields"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Email or phone number already exists")
    })
    @SecurityRequirements
    @PostMapping(ApiConstant.REGISTER_PATH)
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
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
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Login successful – JWT token returned",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid email or password")
    })
    @SecurityRequirements
    @PostMapping(ApiConstant.LOGIN_PATH)
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginrequest,
                                             HttpServletRequest request) {
        String ipAddress = RequestUtils.getClientIp(request);
        String userAgent = RequestUtils.getUserAgent(request);
        LoginResponse loginResponse = authService.login(loginrequest, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, loginResponse, "User"));
    }

    @Operation(
            summary = "Refresh JWT Token",
            description = "Refreshes the JWT access token using a valid refresh token stored in an HTTP-only cookie. Returns a new access token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Token refreshed successfully – new access token returned",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping(ApiConstant.REFRESH_TOKEN_PATH)
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@CookieValue(name = "refresh_token") String refreshToken,
                                                    HttpServletRequest request) {
        String ipAddress = RequestUtils.getClientIp(request);
        String userAgent = RequestUtils.getUserAgent(request);

        RefreshTokenResponse response = refreshTokenService.refreshToken(refreshToken,ipAddress,userAgent);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage. RESOURCE_CREATE, response, "Refresh and Access Token"));
    }

}
