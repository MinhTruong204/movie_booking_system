package com.viecinema.loyalty.controller;

import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.booking.service.VoucherService;
import com.viecinema.common.constant.ApiResponse;
import com.viecinema.loyalty.dto.LoyaltyHistoryItemDto;
import com.viecinema.loyalty.dto.LoyaltySummaryDto;
import com.viecinema.loyalty.dto.request.RedeemPointsRequest;
import com.viecinema.loyalty.dto.response.RedeemPointsResponse;
import com.viecinema.loyalty.dto.response.VoucherDto;
import com.viecinema.loyalty.entity.PointRedemption;
import com.viecinema.loyalty.service.LoyaltyPointsService;
import com.viecinema.loyalty.service.LoyaltyQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.viecinema.common.constant.ApiConstant.*;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_CREATE;

@RestController
@RequestMapping(LOYALTY_PATH)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Loyalty Points", description = "View, track, and redeem user loyalty points")
public class LoyaltyPointsController {

    private final LoyaltyQueryService loyaltyQueryService;
    private final LoyaltyPointsService loyaltyPointsService;
    private final VoucherService voucherService;

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

    // =========================================================================
    // 3. Đổi điểm lấy Voucher/Combo
    // =========================================================================

    @Operation(
            summary = "Redeem loyalty points for a voucher or combo",
            description = """
                    Allows the authenticated user to exchange loyalty points for:
                    - **VOUCHER**: A TICKET_DISCOUNT voucher (reduces ticket price). `pointsToUse` is required.
                    - **COMBO**: A COMBO_DISCOUNT voucher (free combo). `comboId` is required; points are calculated automatically.
                    """,
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201",
                    description = "Points redeemed successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "Not enough points, below minimum threshold, or invalid combo"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Authentication required")
    })
    @PostMapping(LOYALTY_REDEEM_PATH)
    public ResponseEntity<ApiResponse<RedeemPointsResponse>> redeemPoints(
            @Valid @RequestBody RedeemPointsRequest request,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} redeeming points — type={}", currentUser.getId(), request.getRedemptionType());

        PointRedemption redemption = switch (request.getRedemptionType()) {
            case VOUCHER -> loyaltyPointsService.redeemPointsForVoucher(
                    currentUser.getId(),
                    request.getPointsToUse());
            case COMBO -> loyaltyPointsService.redeemPointsForCombo(
                    currentUser.getId(),
                    request.getComboId(),
                    request.getQuantity() != null ? request.getQuantity() : 1);
        };

        RedeemPointsResponse response = buildRedeemResponse(currentUser.getId(), redemption);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(RESOURCE_CREATE, response, "Loyalty points"));
    }

    // =========================================================================
    // 4. Ví Voucher
    // =========================================================================

    @Operation(
            summary = "Get my active vouchers",
            description = "Returns all active vouchers owned by the authenticated user, sorted by expiry date (soonest first). Use this to populate the voucher selection on the booking checkout screen.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
                    description = "Vouchers retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
                    description = "Authentication required")
    })
    @GetMapping(LOYALTY_MY_VOUCHERS_PATH)
    public ResponseEntity<ApiResponse<List<VoucherDto>>> getMyVouchers(
            @RequestParam(required = false) String type,
            @CurrentUser UserPrincipal currentUser) {

        log.info("User {} requesting vouchers (type={})", currentUser.getId(), type);

        List<VoucherDto> vouchers;
        if (type != null && !type.isBlank()) {
            com.viecinema.booking.entity.Voucher.VoucherType voucherType =
                    com.viecinema.booking.entity.Voucher.VoucherType.valueOf(type.toUpperCase());
            vouchers = voucherService.getMyVouchersByType(currentUser.getId(), voucherType);
        } else {
            vouchers = voucherService.getMyVouchers(currentUser.getId());
        }

        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(RESOURCE_RETRIEVED, vouchers, "Vouchers"));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Build RedeemPointsResponse từ PointRedemption.
     * Loyal points service đã cập nhật số dư user, chỉ cần reload.
     */
    private RedeemPointsResponse buildRedeemResponse(Integer userId,
                                                       PointRedemption redemption) {
        com.viecinema.auth.entity.User user = loyaltyQueryService.getUserById(userId);

        RedeemPointsResponse.RedeemPointsResponseBuilder builder = RedeemPointsResponse.builder()
                .redemptionId(redemption.getId())
                .redemptionType(redemption.getRedemptionType())
                .pointsUsed(redemption.getPointsUsed())
                .remainingPoints(user.getLoyaltyPoints())
                .voucherId(redemption.getVoucherId())
                .redeemedAt(redemption.getCreatedAt());

        return builder.build();
    }
}
