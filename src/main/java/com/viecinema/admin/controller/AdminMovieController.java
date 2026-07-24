package com.viecinema.admin.controller;

import com.viecinema.admin.service.AdminMovieService;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.movie.dto.request.CreateMovieRequest;
import com.viecinema.movie.dto.request.MovieFilterRequest;
import com.viecinema.movie.dto.request.UpdateMovieRequest;
import com.viecinema.movie.dto.response.AdminMovieResponse;
import com.viecinema.movie.dto.response.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.viecinema.common.constant.ApiConstant.*;

/**
 * REST Controller cho Admin Movie Management.
 * Tất cả endpoints yêu cầu role ADMIN.
 */
@Slf4j
@RestController
@RequestMapping(ADMIN_MOVIES_PATH)
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - Movie Management", description = "CRUD operations for managing movies (Admin only)")
@SecurityRequirement(name = "bearerAuth")
public class AdminMovieController {

    private final AdminMovieService adminMovieService;

    // ========== CREATE ==========

    @Operation(
            summary = "Tạo phim mới",
            description = "Tạo một bộ phim mới với đầy đủ thông tin, bao gồm thể loại, diễn viên và đạo diễn."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Movie created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Admin only")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AdminMovieResponse>> createMovie(
            @Valid @RequestBody CreateMovieRequest request) {
        log.info("POST {} - Request: {}", ADMIN_MOVIES_PATH, request.getTitle());

        AdminMovieResponse response = adminMovieService.createMovie(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(ApiMessage.RESOURCE_CREATE, response, "Movie"));
    }

    // ========== READ (LIST) ==========

    @Operation(
            summary = "Lấy danh sách phim (Admin)",
            description = "Trả về danh sách phim có phân trang và hỗ trợ filter theo keyword, genre, status. Admin thấy tất cả trạng thái phim."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Movies retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid filter parameters")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AdminMovieResponse>>> getAllMovies(
            @Valid @ModelAttribute MovieFilterRequest request) {
        log.info("GET {} - Filter: {}", ADMIN_MOVIES_PATH, request);

        PagedResponse<AdminMovieResponse> response = adminMovieService.getAllMovies(request);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, response, "Movies"));
    }

    // ========== READ (DETAIL) ==========

    @Operation(
            summary = "Lấy chi tiết phim theo ID",
            description = "Trả về thông tin đầy đủ của một bộ phim, bao gồm audit fields (createdAt, updatedAt)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Movie retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @GetMapping(ADMIN_MOVIE_DETAIL_PATH)
    public ResponseEntity<ApiResponse<AdminMovieResponse>> getMovieById(
            @Parameter(description = "ID của phim", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        log.info("GET {}/{}", ADMIN_MOVIES_PATH, id);

        AdminMovieResponse response = adminMovieService.getMovieById(id);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RETRIEVED, response, "Movie"));
    }

    // ========== UPDATE ==========

    @Operation(
            summary = "Cập nhật thông tin phim",
            description = "Partial update — chỉ các field không null trong request mới được cập nhật. Tự động evict cache sau khi cập nhật."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Movie updated successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @PutMapping(ADMIN_MOVIE_DETAIL_PATH)
    public ResponseEntity<ApiResponse<AdminMovieResponse>> updateMovie(
            @Parameter(description = "ID của phim", required = true, example = "1")
            @PathVariable @Min(1) Integer id,
            @Valid @RequestBody UpdateMovieRequest request) {
        log.info("PUT {}/{} - Request: {}", ADMIN_MOVIES_PATH, id, request);

        AdminMovieResponse response = adminMovieService.updateMovie(id, request);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_UPDATED, response, "Movie"));
    }

    // ========== DELETE ==========

    @Operation(
            summary = "Xóa phim (soft delete)",
            description = "Xóa mềm phim bằng cách set deleted_at. Phim vẫn tồn tại trong database và có thể restore."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Movie deleted successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @DeleteMapping(ADMIN_MOVIE_DETAIL_PATH)
    public ResponseEntity<ApiResponse<Void>> deleteMovie(
            @Parameter(description = "ID của phim", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        log.info("DELETE {}/{}", ADMIN_MOVIES_PATH, id);

        adminMovieService.deleteMovie(id);
        return ResponseEntity.ok(
                ApiResponse.successWithoutData(ApiMessage.RESOURCE_DELETED, "Movie"));
    }

    // ========== RESTORE ==========

    @Operation(
            summary = "Khôi phục phim đã xóa",
            description = "Khôi phục phim đã bị soft-delete bằng cách set deleted_at = NULL."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Movie restored successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Movie is not deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Movie not found")
    })
    @PatchMapping(ADMIN_MOVIE_RESTORE_PATH)
    public ResponseEntity<ApiResponse<AdminMovieResponse>> restoreMovie(
            @Parameter(description = "ID của phim cần khôi phục", required = true, example = "1")
            @PathVariable @Min(1) Integer id) {
        log.info("PATCH {}/{}/restore", ADMIN_MOVIES_PATH, id);

        AdminMovieResponse response = adminMovieService.restoreMovie(id);
        return ResponseEntity.ok(
                ApiResponse.success(ApiMessage.RESOURCE_RESTORED, response, "Movie"));
    }
}
