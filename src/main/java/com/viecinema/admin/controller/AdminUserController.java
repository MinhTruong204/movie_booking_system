package com.viecinema.admin.controller;

import com.viecinema.admin.dto.*;
import com.viecinema.admin.dto.request.*;
import com.viecinema.admin.service.AdminUserService;
import com.viecinema.admin.service.ImportExportService;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.constant.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static com.viecinema.common.constant.ApiConstant.*;

/**
 * REST Controller cho Admin User Management.
 * Tất cả endpoints yêu cầu role ADMIN.
 */
@Slf4j
@RestController
@RequestMapping(ADMIN_USERS_PATH)
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - User Management", description = "CRUD operations for managing users (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final ImportExportService importExportService;

    // ============ CRUD ============

    @Operation(summary = "Get user list", description = "Search, filter, sort and paginate users")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserListDto>>> getUsers(
            @ModelAttribute UserSearchCriteria criteria) {

        Page<AdminUserListDto> users = adminUserService.getUsers(criteria);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, users, "User list"));
    }

    @Operation(summary = "Get user detail", description = "Get full detail of a user by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> getUserById(
            @PathVariable Integer userId) {

        AdminUserDetailDto user = adminUserService.getUserById(userId);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, user, "User detail"));
    }

    @Operation(summary = "Create new user", description = "Create a new user account by admin")
    @PostMapping
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request,
            @CurrentUser UserPrincipal currentUser) {

        AdminUserDetailDto created = adminUserService.createUser(request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ApiMessage.RESOURCE_CREATE, created, "User"));
    }

    @Operation(summary = "Update user", description = "Update user information (partial update)")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> updateUser(
            @PathVariable Integer userId,
            @Valid @RequestBody AdminUpdateUserRequest request,
            @CurrentUser UserPrincipal currentUser) {

        AdminUserDetailDto updated = adminUserService.updateUser(userId, request, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_UPDATED, updated, "User"));
    }

    @Operation(summary = "Soft delete user", description = "Soft delete a user (sets deletedAt)")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> softDeleteUser(
            @PathVariable Integer userId,
            @RequestParam(required = false, defaultValue = "Admin deleted") String reason,
            @CurrentUser UserPrincipal currentUser) {

        adminUserService.softDeleteUser(userId, currentUser.getId(), reason);
        return ResponseEntity.ok(
                ApiResponse.successWithoutData(ApiMessage.RESOURCE_DELETED, "User"));
    }

    @Operation(summary = "Restore deleted user", description = "Restore a soft-deleted user")
    @PostMapping("/{userId}/restore")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> restoreUser(
            @PathVariable Integer userId,
            @CurrentUser UserPrincipal currentUser) {

        AdminUserDetailDto restored = adminUserService.restoreUser(userId, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RESTORED, restored, "User"));
    }

    // ============ Ban / Unban ============

    @Operation(summary = "Ban user", description = "Deactivate/ban a user account")
    @PostMapping("/{userId}/ban")
    public ResponseEntity<ApiResponse<Void>> banUser(
            @PathVariable Integer userId,
            @Valid @RequestBody AdminBanUserRequest request,
            @CurrentUser UserPrincipal currentUser) {

        adminUserService.banUser(userId, request, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.successWithoutData(ApiMessage.USER_BANNED));
    }

    @Operation(summary = "Unban user", description = "Reactivate a banned user account")
    @PostMapping("/{userId}/unban")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> unbanUser(
            @PathVariable Integer userId,
            @CurrentUser UserPrincipal currentUser) {

        AdminUserDetailDto unbanned = adminUserService.unbanUser(userId, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.USER_UNBANNED, unbanned));
    }

    // ============ Role Management ============

    @Operation(summary = "Change user role", description = "Change the role of a user")
    @PutMapping("/{userId}/role")
    public ResponseEntity<ApiResponse<AdminUserDetailDto>> changeRole(
            @PathVariable Integer userId,
            @Valid @RequestBody AdminChangeRoleRequest request,
            @CurrentUser UserPrincipal currentUser) {

        AdminUserDetailDto updated = adminUserService.changeRole(userId, request, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.ROLE_CHANGED, updated));
    }

    // ============ Reset Password ============

    @Operation(summary = "Reset user password", description = "Reset a user's password (revokes all sessions)")
    @PostMapping("/{userId}/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @PathVariable Integer userId,
            @Valid @RequestBody AdminResetPasswordRequest request,
            @CurrentUser UserPrincipal currentUser) {

        adminUserService.resetPassword(userId, request, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.successWithoutData(ApiMessage.PASSWORD_RESET_SUCCESS));
    }

    // ============ Session Management ============

    @Operation(summary = "Get user sessions", description = "View all active sessions (refresh tokens) of a user")
    @GetMapping("/{userId}/sessions")
    public ResponseEntity<ApiResponse<List<UserSessionDto>>> getUserSessions(
            @PathVariable Integer userId) {

        List<UserSessionDto> sessions = adminUserService.getUserSessions(userId);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, sessions, "User sessions"));
    }

    @Operation(summary = "Revoke a session", description = "Revoke a specific session of a user")
    @DeleteMapping("/{userId}/sessions/{tokenId}")
    public ResponseEntity<ApiResponse<Void>> revokeSession(
            @PathVariable Integer userId,
            @PathVariable Long tokenId) {

        adminUserService.revokeSession(userId, tokenId);
        return ResponseEntity.ok(
                ApiResponse.successWithoutData(ApiMessage.SESSIONS_REVOKED));
    }

    @Operation(summary = "Revoke all sessions", description = "Revoke all sessions of a user")
    @DeleteMapping("/{userId}/sessions")
    public ResponseEntity<ApiResponse<Void>> revokeAllSessions(
            @PathVariable Integer userId,
            @CurrentUser UserPrincipal currentUser) {

        adminUserService.revokeAllSessions(userId, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.successWithoutData(ApiMessage.SESSIONS_REVOKED));
    }

    // ============ Dashboard ============

    @Operation(summary = "Dashboard overview", description = "Get user statistics and charts for admin dashboard")
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardDto>> getDashboard() {

        AdminDashboardDto dashboard = adminUserService.getDashboard();
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, dashboard, "Dashboard"));
    }

    // ============ Import / Export ============

    @Operation(summary = "Export users to CSV", description = "Export user list to CSV file (supports filtering)")
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportToCSV(@ModelAttribute UserSearchCriteria criteria) throws IOException {

        ByteArrayOutputStream outputStream = importExportService.exportUsersToCSV(criteria);
        return buildFileResponse(outputStream.toByteArray(), "users.csv", "text/csv");
    }

    @Operation(summary = "Export users to Excel", description = "Export user list to Excel file (supports filtering)")
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(@ModelAttribute UserSearchCriteria criteria) throws IOException {

        ByteArrayOutputStream outputStream = importExportService.exportUsersToExcel(criteria);
        return buildFileResponse(outputStream.toByteArray(), "users.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    @Operation(summary = "Import users from CSV", description = "Import users from a CSV file")
    @PostMapping(value = "/import/csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResult>> importFromCSV(
            @Parameter(description = "CSV file") @RequestParam("file") MultipartFile file,
            @CurrentUser UserPrincipal currentUser) throws IOException {

        ImportResult result = importExportService.importUsersFromCSV(file, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.IMPORT_COMPLETED, result,
                        String.valueOf(result.getSuccessCount()),
                        String.valueOf(result.getFailedCount())));
    }

    @Operation(summary = "Import users from Excel", description = "Import users from an Excel (.xlsx) file")
    @PostMapping(value = "/import/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ImportResult>> importFromExcel(
            @Parameter(description = "Excel file") @RequestParam("file") MultipartFile file,
            @CurrentUser UserPrincipal currentUser) throws IOException {

        ImportResult result = importExportService.importUsersFromExcel(file, currentUser.getId());
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.IMPORT_COMPLETED, result,
                        String.valueOf(result.getSuccessCount()),
                        String.valueOf(result.getFailedCount())));
    }

    @Operation(summary = "Download import template", description = "Download a CSV template for user import")
    @GetMapping("/import/template")
    public ResponseEntity<byte[]> getImportTemplate() throws IOException {

        ByteArrayOutputStream outputStream = importExportService.getImportTemplate();
        return buildFileResponse(outputStream.toByteArray(), "user_import_template.csv", "text/csv");
    }

    // ============ Helper ============

    private ResponseEntity<byte[]> buildFileResponse(byte[] data, String filename, String contentType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename).build());
        headers.setContentLength(data.length);
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}
