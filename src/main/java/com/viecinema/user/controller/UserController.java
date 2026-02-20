package com.viecinema.user.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.user.dto.UserProfileDto;
import com.viecinema.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.viecinema.common.constant.ApiConstant.USER_PATH;
import static com.viecinema.common.constant.ApiConstant.USER_PROFILE_PATH;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@Slf4j
@RestController
@RequestMapping(USER_PATH)
@RequiredArgsConstructor
@Tag(name = "Users", description = "Manage user account information")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "Get current user profile",
            description = "Returns the profile information of the currently authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping(USER_PROFILE_PATH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUserProfile(
            @CurrentUser UserPrincipal currentUser) {

        log.info("GET /api/users/profile - User: {}", currentUser.getUsername());
        Integer userId = currentUser.getId();
        UserProfileDto profile = userService.getUserProfile(userId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(RESOURCE_RETRIEVED, profile, "User profile"));
    }
}

