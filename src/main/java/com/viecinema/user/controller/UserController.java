package com.viecinema.user.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.user.dto.UserProfileDto;
import com.viecinema.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
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
public class UserController {

    private final UserService userService;

    @GetMapping(USER_PROFILE_PATH)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUserProfile(
            @CurrentUser UserPrincipal currentUser) {

        log.info("GET /api/users/profile - User: {}", currentUser.getUsername());

        // Extract userId từ UserDetails
        // Giả sử UserDetails implementation có method getUserId()
        // Hoặc parse từ username nếu bạn lưu userId vào username
        Integer userId = currentUser.getId();

        UserProfileDto profile = userService.getUserProfile(userId);

        return ResponseEntity.status(HttpStatus.OK).body(ApiResponse.success(RESOURCE_RETRIEVED, profile,"User profile"));

    }

}
