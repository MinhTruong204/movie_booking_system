package com.viecinema.payment.controller;

import com.viecinema.common.constant.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.payment.dto.request.VnpayPaymentRequest;
import com.viecinema.payment.dto.response.PaymentInfoResponse;
import com.viecinema.payment.dto.response.VnpayCallbackResponse;
import com.viecinema.payment.dto.response.VnpayPaymentResponse;
import com.viecinema.payment.service.PaymentService;
import com.viecinema.payment.service.VnpayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Map;

import static com.viecinema.common.constant.ApiConstant.*;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_CREATE;
import static com.viecinema.common.constant.ApiMessage.RESOURCE_RETRIEVED;

@Slf4j
@RestController
@RequestMapping(PAYMENT_PATH)
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Manage payments via VNPay payment gateway")
public class PaymentController {

    private final VnpayService vnpayService;
    private final PaymentService paymentService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Lấy thông tin thanh toán từ booking ID
     */
    @Operation(
            summary = "Get payment info by booking ID",
            description = "Returns the payment details associated with a specific booking. Only the booking owner can access this endpoint.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Payment info retrieved successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Booking not found")
    })
    @GetMapping(PAYMENT_DETAIL_PATH)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<PaymentInfoResponse>> getPaymentByBookingId(
            @CurrentUser UserPrincipal currentUser,
            @Parameter(description = "ID of the booking", required = true, example = "1")
            @PathVariable Integer bookingId,
            HttpServletRequest httpRequest) {

        log.info("User {} getting payment info for booking {}", currentUser.getId(), bookingId);
        PaymentInfoResponse response = paymentService.getPaymentByBookingId(bookingId, httpRequest);
        return ResponseEntity.ok(ApiResponse.success(RESOURCE_RETRIEVED, response, "Payment"));
    }

    /**
     * Tạo URL thanh toán VNPay
     */
    @Operation(
            summary = "Create VNPay payment URL",
            description = "Generates a VNPay payment URL for the specified booking. The user should be redirected to the returned URL to complete payment.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Payment URL created successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Booking already paid or invalid state"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Authentication required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "CUSTOMER role required")
    })
    @PostMapping(VNPAY_CREATE_PATH)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<VnpayPaymentResponse>> createVnpayPayment(
            @CurrentUser UserPrincipal currentUser,
            @Parameter(description = "ID of the booking to pay for", required = true, example = "1")
            @RequestParam Integer bookingId,
            @RequestBody(required = false) VnpayPaymentRequest request,
            HttpServletRequest httpRequest) {

        log.info("User {} creating VNPay payment for booking {}", currentUser.getId(), bookingId);

        if (request == null) {
            request = new VnpayPaymentRequest();
        }
        request.setBookingId(bookingId);

        VnpayPaymentResponse response = vnpayService.createPayment(bookingId, request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(RESOURCE_CREATE, response, "Payment"));
    }

    /**
     * Callback từ VNPay (Return URL)
     * VNPay sẽ redirect user về đây sau khi thanh toán
     */
    @Operation(
            summary = "VNPay return URL (callback)",
            description = "This endpoint is called by VNPay to redirect the user back after payment. It validates the result and redirects to the frontend success or failure page. **Not intended to be called directly.**"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "Redirects to frontend payment result page")
    })
    @SecurityRequirements
    @GetMapping(VNPAY_CALLBACK_PATH)
    public void vnpayCallback(
            @Parameter(description = "VNPay response parameters (appended automatically by VNPay)")
            @RequestParam Map<String, String> params,
            HttpServletResponse response) throws IOException {

        log.info("VNPay callback received with params: {}", params);
        VnpayCallbackResponse callbackResponse = vnpayService.handleCallback(params);

        String redirectUrl;
        if ("00".equals(callbackResponse.getCode())) {
            redirectUrl = String.format("%s/booking/payment-success?bookingCode=%s&transactionNo=%s",
                    frontendUrl,
                    callbackResponse.getBookingCode(),
                    callbackResponse.getTransactionNo());
        } else {
            redirectUrl = String.format("%s/booking/payment-failed?bookingCode=%s&code=%s&message=%s",
                    frontendUrl,
                    callbackResponse.getBookingCode(),
                    callbackResponse.getCode(),
                    callbackResponse.getMessage());
        }
        response.sendRedirect(redirectUrl);
    }

    /**
     * IPN (Instant Payment Notification) từ VNPay
     * VNPay gọi API này từ server, không qua browser
     */
    @Operation(
            summary = "VNPay IPN handler",
            description = "Instant Payment Notification endpoint called server-to-server by VNPay to confirm payment status. **Not intended to be called directly.**"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "IPN processed successfully")
    })
    @SecurityRequirements
    @PostMapping(VNPAY_IPN_PATH)
    public ResponseEntity<Map<String, String>> vnpayIPN(
            @Parameter(description = "VNPay IPN parameters")
            @RequestParam Map<String, String> params) {
        log.info("VNPay IPN received");
        Map<String, String> response = vnpayService.handleIPN(params);

        return ResponseEntity.ok(response);
    }
}