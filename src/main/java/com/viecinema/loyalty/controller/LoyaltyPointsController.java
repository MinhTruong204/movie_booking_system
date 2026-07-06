package com.viecinema.loyalty.controller;

import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.loyalty.dto.LoyaltyHistoryItemDto;
import com.viecinema.loyalty.dto.LoyaltySummaryDto;
import com.viecinema.loyalty.service.LoyaltyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static com.viecinema.common.constant.ApiConstant.*;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@RestController
@RequestMapping(LOYALTY_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loyalty Points", description = "View and track user loyalty points")
public class LoyaltyPointsController {

    private final LoyaltyQueryService loyaltyQueryService;

    @Operation(
            summary = "Get my loyalty points summary",
            description = "Returns the current points balance, membership tier, and progress to the next tier for the authenticated user.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Points summary retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping(LOYALTY_MY_POINTS_PATH)
    public ResponseEntity<ApiResponse<LoyaltySummaryDto>> getMyPoints(
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} requesting loyalty points summary", currentUser.getId());
        LoyaltySummaryDto summary = loyaltyQueryService.getSummary(currentUser.getId());
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(RESOURCE_RETRIEVED, summary, "Loyalty points summary"));
    }

    @Operation(
            summary = "Get loyalty points history",
            description = "Returns a paginated history of all points earned and redeemed by the authenticated user, ordered by most recent first.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "History retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required")
    })
    @GetMapping(LOYALTY_HISTORY_PATH)
    public ResponseEntity<ApiResponse<Page<LoyaltyHistoryItemDto>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @CurrentUser UserPrincipal currentUser) {

        size = Math.min(size, 100);
        log.info("User {} requesting loyalty history — page={}, size={}", currentUser.getId(), page, size);

        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<LoyaltyHistoryItemDto> history = loyaltyQueryService.getHistory(currentUser.getId(), pageable);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(RESOURCE_RETRIEVED, history, "Loyalty points history"));
    }
}
