package com.viecinema.booking.dto.response;

import com.viecinema.booking.dto.BookingComboInfo;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO trả về chi tiết đầy đủ một vé đặt cho người dùng đã xác thực.
 * Bao gồm thông tin suất chiếu, ghế, combo, giá tiền, QR code và check-in.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingDetailResponse {

    // ── Thông tin booking ──
    private Integer bookingId;
    private String bookingCode;
    private BookingStatus status;

    // ── Thông tin suất chiếu & phim ──
    private ShowtimeInfo showtime;

    // ── Ghế & Combo ──
    private List<SeatInfo> seats;
    private List<BookingComboInfo> combos;

    // ── Tóm tắt giá (đọng lại từ DB, không tính lại) ──
    private BigDecimal totalAmount;    // Giá gốc trước giảm giá
    private BigDecimal finalAmount;    // Số tiền thực tế phải trả

    // ── QR code (chỉ hiển thị khi CONFIRMED / CHECKED_IN) ──
    private String qrCodeData;
    private String qrCodeImageUrl;
    /** Ảnh QR Code dạng Base64 PNG. Dùng trực tiếp: {@code <img src="data:image/png;base64,{qrCodeBase64}"/>} */
    private String qrCodeBase64;

    // ── Thông tin check-in ──
    private LocalDateTime checkedInAt;
    private String checkedInLocation;

    // ── Điểm loyalty đã sử dụng ──
    private Integer loyaltyPointsUsed;

    // ── Thời gian ──
    private LocalDateTime bookedAt;       // createdAt của booking
    private LocalDateTime expiresAt;      // Thời hạn thanh toán (PENDING)
}
