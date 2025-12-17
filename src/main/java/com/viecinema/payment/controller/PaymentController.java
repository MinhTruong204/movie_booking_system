package com.viecinema.payment.controller;

import com.viecinema.auth.dto.response.ApiResponse;
import com.viecinema.auth.security.CurrentUser;
import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.payment.dto.request.VnpayPaymentRequest;
import com.viecinema.payment.dto.response.VnpayCallbackResponse;
import com.viecinema.payment.dto.response.VnpayPaymentResponse;
import com.viecinema.payment.service.VnpayService;
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
import static com.viecinema.common.constant.ApiMessage.PAYMENT_CREATED;

@Slf4j
@RestController
@RequestMapping(PAYMENT_PATH)
@RequiredArgsConstructor
public class PaymentController {

    private final VnpayService vnpayService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Tạo URL thanh toán VNPay
     */
    @PostMapping(VNPAY_CREATE_PATH)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<ApiResponse<VnpayPaymentResponse>> createVnpayPayment(
            @CurrentUser UserPrincipal currentUser,
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
                ApiResponse.success(PAYMENT_CREATED,response));
    }

    /**
     * Callback từ VNPay (Return URL)
     * VNPay sẽ redirect user về đây sau khi thanh toán
     */
    @GetMapping(VNPAY_CALLBACK_PATH)
    public void vnpayCallback(
            @RequestParam Map<String, String> params,
            HttpServletResponse response) throws IOException {

        log.info("VNPay callback received with params: {}", params);

        VnpayCallbackResponse callbackResponse = vnpayService.handleCallback(params);

        // Redirect về frontend với kết quả
        String redirectUrl;
        if ("00".equals(callbackResponse.getCode())) {
            // Thành công -> redirect về trang success
            redirectUrl = String.format("%s/booking/payment-success?bookingCode=%s&transactionNo=%s",
                    frontendUrl,
                    callbackResponse.getBookingCode(),
                    callbackResponse.getTransactionNo()
            );
        } else {
            // Thất bại -> redirect về trang failed
            redirectUrl = String.format("%s/booking/payment-failed?bookingCode=%s&code=%s&message=%s",
                    frontendUrl,
                    callbackResponse.getBookingCode(),
                    callbackResponse.getCode(),
                    callbackResponse.getMessage()
            );
        }

        response.sendRedirect(redirectUrl);
    }

    /**
     * IPN (Instant Payment Notification) từ VNPay
     * VNPay gọi API này từ server, không qua browser
     */
    @PostMapping(VNPAY_IPN_PATH)
    public ResponseEntity<Map<String, String>> vnpayIPN(@RequestParam Map<String, String> params) {
        log.info("VNPay IPN received");

        Map<String, String> response = vnpayService.handleIPN(params);

        return ResponseEntity.ok(response);
    }
}