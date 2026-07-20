package com.viecinema.auth.controller;

import com.viecinema.auth.dto.request.ForgotPasswordRequest;
import com.viecinema.auth.dto.request.LoginRequest;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.request.ResendVerificationRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@RequestMapping(ApiConstant.AUTH_PATH)
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration and login")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Operation(
            summary = "Register a new user",
            description = "Creates a new customer account and sends a verification email. Returns the created user details."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully. A verification email has been sent.",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Validation error - invalid input fields"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "Email or phone number already exists")
    })
    @SecurityRequirements
    @PostMapping(ApiConstant.REGISTER_PATH)
    public ResponseEntity<ApiResponse<RegisterResponse>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        RegisterResponse registerResponse = authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ApiMessage.REGISTER_SUCCESS, registerResponse));
    }

    @Operation(
            summary = "Verify email address",
            description = "Verifies the user's email using the token sent to their inbox. Redirects to the frontend on success."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "302", description = "Email verified successfully - redirected to frontend"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Invalid or expired verification token")
    })
    @SecurityRequirements
    @GetMapping(ApiConstant.VERIFY_EMAIL_PATH)
    public RedirectView verifyEmail(@RequestParam("token") String token) {
        authService.verifyEmail(token);
        return new RedirectView(frontendUrl + "/verify-email-success");
    }

    @Operation(
            summary = "Resend verification email",
            description = "Sends a new verification email to the given address. Invalidates any existing token."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Verification email resent successfully"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Email is already verified"),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found")
    })
    @SecurityRequirements
    @PostMapping(ApiConstant.RESEND_VERIFICATION_PATH)
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED,
                        null, "Verification email has been sent to " + request.getEmail()));
    }

    @Operation(
            summary = "Login",
            description = "Authenticates a user with email and password. Returns a JWT access token. Email must be verified first."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Login successful - JWT token returned",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Email not verified or account locked"),

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
                    responseCode = "200", description = "Token refreshed successfully - new access token returned",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),

            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping(ApiConstant.REFRESH_TOKEN_PATH)
    public ResponseEntity<ApiResponse<RefreshTokenResponse>> refreshToken(@CookieValue(name = "refresh_token") String refreshToken,
                                                    HttpServletRequest request) {
        String ipAddress = RequestUtils.getClientIp(request);
        String userAgent = RequestUtils.getUserAgent(request);

        RefreshTokenResponse response = refreshTokenService.refreshToken(refreshToken, ipAddress, userAgent);
        return ResponseEntity.status(HttpStatus.OK).body(
                ApiResponse.success(ApiMessage.RESOURCE_CREATE, response, "Refresh and Access Token"));
    }

    @Operation(
            summary = "Forgot password",
            description = "Generates a temporary password and sends it to the specified registered email address."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Temporary password email sent successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "User not found")
    })
    @SecurityRequirements
    @PostMapping(ApiConstant.FORGOT_PASSWORD_PATH)
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, null,
                        "A temporary password has been sent to " + request.getEmail()));
    }

}
