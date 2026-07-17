package com.viecinema.booking.service;

import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.BookingCombo;
import com.viecinema.booking.entity.BookingSeat;
import com.viecinema.booking.repository.BookingComboRepository;
import com.viecinema.booking.repository.BookingRepository;
import com.viecinema.common.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service chuyen gui email xac nhan dat ve kem QR Code.
 *
 * Nhan vao bookingId (Integer) thay vi Booking entity de tranh LazyInitializationException:
 * khi @Async chay tren thread moi, Hibernate session cu da dong nen cac
 * lazy collection (bookingSeats, showtime.movie...) se bi loi neu dung entity cu.
 * Giai phap: load lai Booking voi EntityGraph day du trong @Transactional moi.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;
    private final BookingRepository bookingRepository;
    private final BookingComboRepository bookingComboRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Gui email xac nhan dat ve thanh cong kem QR Code checkin.
     *
     * @param bookingId ID cua don dat ve vua thanh toan thanh cong
     */
    @Async
    @Transactional(readOnly = true)
    public void sendBookingConfirmationEmail(Integer bookingId) {
        try {
            log.info("[BookingEmail] Bat dau gui email xac nhan cho bookingId: {}", bookingId);

            // 1. Load lai Booking voi day du associations (tranh LazyInitializationException)
            Booking booking = bookingRepository.findByIdForEmail(bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

            // 2. Sinh anh QR Code tu du lieu booking
            byte[] qrImageBytes = QrCodeUtil.generateQrCode(booking.getQrCodeData());

            // 3. Lay danh sach combo
            List<BookingCombo> combos = bookingComboRepository.findByBooking(booking);

            // Tinh tong tien ve
            BigDecimal ticketTotal = booking.getBookingSeats() == null ? BigDecimal.ZERO
                    : booking.getBookingSeats().stream()
                        .map(BookingSeat::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Tinh tong tien combo
            BigDecimal comboTotal = combos.stream()
                    .map(bc -> bc.getPrice().multiply(BigDecimal.valueOf(bc.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Discount = totalAmount - finalAmount
            BigDecimal discount = booking.getTotalAmount().subtract(booking.getFinalAmount());

            // Danh sach ghe label sap xep (A1, B3...)
            List<String> seatLabels = booking.getBookingSeats() == null ? List.of()
                    : booking.getBookingSeats().stream()
                        .map(bs -> bs.getSeat().getSeatLabel())
                        .sorted()
                        .collect(Collectors.toList());

            // 4. Build Thymeleaf context
            Context context = new Context();
            context.setVariable("fullName", booking.getUser().getFullName());
            context.setVariable("bookingCode", booking.getBookingCode());
            context.setVariable("bookingDate", booking.getCreatedAt().format(DATE_FORMATTER));

            // Movie info
            context.setVariable("movieTitle", booking.getShowtime().getMovie().getTitle());
            context.setVariable("moviePoster", booking.getShowtime().getMovie().getPosterUrl());
            context.setVariable("movieDuration", booking.getShowtime().getMovie().getDuration());
            context.setVariable("ageRating", booking.getShowtime().getMovie().getAgeRating());

            // Showtime info
            context.setVariable("startTime", booking.getShowtime().getStartTime().format(DATE_TIME_FORMATTER));
            context.setVariable("endTime", booking.getShowtime().getEndTime().format(DATE_TIME_FORMATTER));
            context.setVariable("roomName", booking.getShowtime().getRoom().getName());
            context.setVariable("cinemaName", booking.getShowtime().getRoom().getCinema().getName());
            context.setVariable("cinemaAddress", booking.getShowtime().getRoom().getCinema().getAddress());
            context.setVariable("cinemaCity", booking.getShowtime().getRoom().getCinema().getCity());

            // Seat & combo info
            context.setVariable("bookingSeats", booking.getBookingSeats());
            context.setVariable("seatLabels", seatLabels);
            context.setVariable("bookingCombos", combos);

            // Pricing
            context.setVariable("ticketTotal", ticketTotal);
            context.setVariable("comboTotal", comboTotal);
            context.setVariable("discount", discount);
            context.setVariable("finalAmount", booking.getFinalAmount());
            context.setVariable("loyaltyPointsUsed", booking.getLoyaltyPointsUsed());

            // 5. Render HTML template
            String htmlContent = templateEngine.process("email/booking_confirmation", context);

            // 6. Tao MimeMessage voi QR Code dinh kem inline
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "VieCinema");
            helper.setTo(booking.getUser().getEmail());
            helper.setSubject("[VieCinema] Dat ve thanh cong | " + booking.getBookingCode());
            helper.setText(htmlContent, true);

            // Dinh kem QR Code inline voi Content-ID = "qrcode"
            helper.addInline("qrcode", new ByteArrayResource(qrImageBytes), "image/png");

            mailSender.send(message);
            log.info("[BookingEmail] Send successfully to: {} | Booking: {}",
                    booking.getUser().getEmail(), booking.getBookingCode());

        } catch (Exception e) {
            // Khong throw exception de khong anh huong den luong thanh toan
            log.error("[BookingEmail] Failed to send email for bookingId {}: {}",
                    bookingId, e.getMessage(), e);
        }
    }
}