package com.viecinema.booking.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.dto.BookingComboInfo;
import com.viecinema.booking.dto.SelectedCombo;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.booking.dto.PricingContext;
import com.viecinema.booking.dto.request.BookingRequest;
import com.viecinema.booking.dto.request.GuestBookingRequest;
import com.viecinema.booking.dto.response.BookingDetailResponse;
import com.viecinema.booking.dto.response.BookingResponse;
import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.BookingCombo;
import com.viecinema.booking.entity.BookingSeat;
import com.viecinema.booking.entity.Combo;
import com.viecinema.booking.repository.BookingComboRepository;
import com.viecinema.booking.repository.BookingRepository;
import com.viecinema.booking.repository.BookingSeatRepository;
import com.viecinema.booking.repository.ComboRepository;
import com.viecinema.booking.validator.BookingValidator;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.common.enums.Role;
import com.viecinema.common.enums.SeatStatusType;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.util.QrCodeUtil;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import com.viecinema.showtime.entity.Seat;
import com.viecinema.showtime.entity.SeatStatus;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.SeatStatusRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {
    private static final int PAYMENT_TIMEOUT_MINUTES = 10;
    private final BookingRepository bookingRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingComboRepository bookingComboRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ComboRepository comboRepository;
    private final UserRepository userRepository;
    private final BookingCalculationService bookingCalculationService;
    private final BookingValidator bookingValidator;
    private final PromotionValidationService promotionValidationService;
    private final VoucherService voucherService;

    @Transactional
    public BookingResponse createBooking(Integer userId, BookingRequest request) {
        log.info("Creating booking for user {} - showtime {}", userId, request.getShowtimeId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime"));

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        Map<Combo, Integer> selectedCombos = resolveCombos(request.getCombos());

        bookingValidator.validateUser(user);
        bookingValidator.validateShowtime(showtime);
        bookingValidator.validateSeatAvailability(request.getShowtimeId(), request.getSeatIds(), userId);


        PricingContext context = PricingContext.builder()
                .user(user)
                .showtime(showtime)
                .seats(seats)
                .selectedCombos(selectedCombos != null ? selectedCombos : new HashMap<>())
                .promoCode(request.getPromoCode())
                .voucherCode(request.getVoucherCode())
                .useLoyaltyPoints(0)
                .ticketVoucherId(request.getTicketVoucherId())
                .comboVoucherId(request.getComboVoucherId())
                .build();

        PriceBreakdown priceBreakdown = bookingCalculationService.calculatePrice(context);

        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .bookingCode(generateBookingCode())
                .status(BookingStatus.PENDING)
                .totalAmount(priceBreakdown.getSubtotal())
                .finalAmount(priceBreakdown.getFinalAmount())
                .build();

        booking = bookingRepository.save(booking);

        // Save booking seats and combos
        List<BookingSeat> bookingSeats = saveBookingSeats(booking, seats, showtime);
        List<BookingCombo> bookingCombos = saveBookingCombos(booking, selectedCombos);
        booking.setBookingSeats(bookingSeats);
        updateSeatStatus(request.getShowtimeId(), request.getSeatIds(), booking);

        // Bước 4: Commit promotion nếu có mã KM
        if (StringUtils.hasText(request.getPromoCode())) {
            promotionValidationService.commitPromotion(
                    booking,
                    request.getPromoCode(),
                    priceBreakdown.getPromoDiscount()
            );
        }

        // Bước 5: Commit voucher TICKET_DISCOUNT nếu có
        if (request.getTicketVoucherId() != null
                && priceBreakdown.getTicketVoucherDiscount() != null
                && priceBreakdown.getTicketVoucherDiscount().compareTo(BigDecimal.ZERO) > 0) {
            voucherService.commitVoucherUsage(
                    booking,
                    request.getTicketVoucherId(),
                    priceBreakdown.getTicketVoucherDiscount(),
                    com.viecinema.booking.entity.Voucher.VoucherType.TICKET_DISCOUNT
            );
        }

        // Bước 6: Commit voucher COMBO_DISCOUNT nếu có
        if (request.getComboVoucherId() != null
                && priceBreakdown.getComboVoucherDiscount() != null
                && priceBreakdown.getComboVoucherDiscount().compareTo(BigDecimal.ZERO) > 0) {
            voucherService.commitVoucherUsage(
                    booking,
                    request.getComboVoucherId(),
                    priceBreakdown.getComboVoucherDiscount(),
                    com.viecinema.booking.entity.Voucher.VoucherType.COMBO_DISCOUNT
            );
        }

        // Build response
        BookingResponse response = buildBookingResponse(
                booking,
                showtime,
                bookingSeats,
                bookingCombos,
                priceBreakdown
        );

        log.info("Booking created successfully: {}", booking.getBookingCode());
        return response;
    }

    @Transactional
    public BookingResponse createBookingForGuest(GuestBookingRequest request) {
        log.info("Creating booking for guest {} - showtime {}", request.getEmail(), request.getShowtimeId());

        User user = userRepository.findByEmail(request.getEmail());
        if(user == null) {
            user = User.builder()
                    .email(request.getEmail())
                    .phone(request.getPhoneNumber())
                    .fullName(request.getFullName())
                    .role(Role.GUEST)
                    .passwordHash("guestpass")
                    .build();
            user = userRepository.save(user);
        }
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime"));

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        Map<Combo, Integer> selectedCombos = resolveCombos(request.getCombos());

        // bookingValidator.validateUser(user);
        bookingValidator.validateShowtime(showtime);
        bookingValidator.validateSeatAvailability(request.getShowtimeId(), request.getSeatIds(), user.getId());


        PricingContext context = PricingContext.builder()
                .user(user)
                .showtime(showtime)
                .seats(seats)
                .selectedCombos(selectedCombos != null ? selectedCombos : new HashMap<>())
                .promoCode(request.getPromoCode())
                .voucherCode(request.getVoucherCode())
                .useLoyaltyPoints(0)
                .ticketVoucherId(null)
                .comboVoucherId(null)
                .build();

        PriceBreakdown priceBreakdown = bookingCalculationService.calculatePrice(context);

        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .bookingCode(generateBookingCode())
                .status(BookingStatus.PENDING)
                .totalAmount(priceBreakdown.getSubtotal())
                .finalAmount(priceBreakdown.getFinalAmount())
                .build();

        booking = bookingRepository.save(booking);

        // Save booking seats and combos
        List<BookingSeat> bookingSeats = saveBookingSeats(booking, seats, showtime);
        List<BookingCombo> bookingCombos = saveBookingCombos(booking, selectedCombos);
        booking.setBookingSeats(bookingSeats);
        updateSeatStatus(request.getShowtimeId(), request.getSeatIds(), booking);

        // Bước 4: Commit promotion nếu có mã KM
        if (StringUtils.hasText(request.getPromoCode())) {
            promotionValidationService.commitPromotion(
                    booking,
                    request.getPromoCode(),
                    priceBreakdown.getPromoDiscount()
            );
        }


        // Build response
        BookingResponse response = buildBookingResponse(
                booking,
                showtime,
                bookingSeats,
                bookingCombos,
                priceBreakdown
        );

        log.info("Booking created successfully: {}", booking.getBookingCode());
        return response;
    } 

    // ========== PUBLIC READ METHODS ==========

    /**
     * Lấy chi tiết một vé đặt theo bookingId.
     *
     * <p><b>Xác thực quyền sở hữu:</b> Chỉ chủ sở hữu booking hoặc ADMIN mới được phép truy cập.
     * Nếu booking không tồn tại → {@link com.viecinema.common.exception.ResourceNotFoundException}.
     * Nếu người dùng không có quyền → {@link AccessDeniedException}.
     *
     * @param bookingId  ID của booking cần lấy.
     * @param requesterId ID của user đang thực hiện request (lấy từ JWT).
     * @param isAdmin    True nếu caller có role ADMIN.
     * @return Chi tiết đầy đủ của booking.
     */
    @Transactional(readOnly = true)
    public BookingDetailResponse getBookingDetail(Integer bookingId, Integer requesterId, boolean isAdmin) {
        log.info("User {} fetching booking detail for bookingId={}", requesterId, bookingId);

        // 1. Load booking với đầy đủ associations (tránh LazyInitializationException)
        Booking booking = bookingRepository.findByIdWithFullDetails(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking"));

        // 2. Xác thực quyền: chỉ chủ sở hữu hoặc ADMIN được xem
        boolean isOwner = booking.getUser().getId().equals(requesterId);
        if (!isOwner && !isAdmin) {
            log.warn("Access denied: user {} attempted to view booking {} owned by user {}",
                    requesterId, bookingId, booking.getUser().getId());
            throw new AccessDeniedException("You do not have permission to view this booking");
        }

        // 3. Load combos riêng (bookingSeats đã được load qua EntityGraph)
        List<BookingCombo> bookingCombos = bookingComboRepository.findByBooking(booking);

        // 4. Build và trả về response
        return buildBookingDetailResponse(booking, bookingCombos);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private String generateBookingCode() {
        // Format: BK + YYYYMMDD + Random 6 digits
        String datePart = LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%06d", new Random().nextInt(1000000));
        return "BK" + datePart + randomPart;
    }

    private List<BookingSeat> saveBookingSeats(Booking booking, List<Seat> seats, Showtime showtime) {
        List<BookingSeat> bookingSeats = new ArrayList<>();

        for (Seat seat : seats) {
            BigDecimal seatPrice = showtime.getBasePrice()
                    .multiply(seat.getSeatType().getPriceMultiplier());

            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .price(seatPrice)
                    .build();

            bookingSeats.add(bookingSeatRepository.save(bookingSeat));
        }

        return bookingSeats;
    }

    private List<BookingCombo> saveBookingCombos(
            Booking booking,
            Map<Combo, Integer> comboMap) {

        if (comboMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<BookingCombo> listToSave = new ArrayList<>();

        for (Map.Entry<Combo, Integer> entry : comboMap.entrySet()) {
            Combo combo = entry.getKey();
            Integer quantity = entry.getValue();
            BookingCombo bookingCombo = BookingCombo.builder()
                    .booking(booking)
                    .combo(combo)
                    .quantity(quantity)
                    .price(combo.getPrice())
                    .build();
            listToSave.add(bookingCombo);
        }

        return bookingComboRepository.saveAll(listToSave);
    }


    private void updateSeatStatus(Integer showtimeId, List<Integer> seatIds, Booking booking) {
        List<SeatStatus> seatStatuses = seatStatusRepository
                .findByShowtimeAndSeatsWithLock(showtimeId, seatIds);

        for (SeatStatus seatStatus : seatStatuses) {
            seatStatus.setStatus(SeatStatusType.BOOKED);
            seatStatus.setHeldByUser(null);
            seatStatus.setHeldUntil(null);
        }
    }

    private BookingResponse buildBookingResponse(
            Booking booking,
            Showtime showtime,
            List<BookingSeat> bookingSeats,
            List<BookingCombo> bookingCombos,
            PriceBreakdown priceBreakdown) {

        // Showtime info
        ShowtimeInfo showtimeInfo = ShowtimeInfo.builder()
                .showtimeId(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .posterUrl(showtime.getMovie().getPosterUrl())
                .build();

        // Seats info
        List<SeatInfo> seatsInfo = bookingSeats.stream()
                .map(bs -> SeatInfo.builder()
                        .seatId(bs.getSeat().getSeatId())
                        .rowLabel(bs.getSeat().getSeatRow())
                        .seatNumber(bs.getSeat().getSeatNumber())
                        .seatTypeName(bs.getSeat().getSeatType().getName())
                        .price(bs.getPrice())
                        .build())
                .collect(Collectors.toList());

        // Combos info
        List<BookingComboInfo> combosInfo = bookingCombos.stream()
                .map(bc -> BookingComboInfo.builder()
                        .comboId(bc.getCombo().getId())
                        .comboName(bc.getCombo().getName())
                        .quantity(bc.getQuantity())
                        .unitPrice(bc.getPrice())
                        .totalPrice(bc.getPrice().multiply(BigDecimal.valueOf(bc.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        return BookingResponse.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus())
                .showtime(showtimeInfo)
                .seats(seatsInfo)
                .combos(combosInfo)
                .priceBreakdown(priceBreakdown)
                .createdAt(booking.getCreatedAt())
                .expiresAt(booking.getCreatedAt().plusMinutes(PAYMENT_TIMEOUT_MINUTES))
                .build();
    }

    private Map<Combo, Integer> resolveCombos(List<SelectedCombo> requestCombos) {
        if (requestCombos == null) {
            return new HashMap<>();
        }
        Map<Integer, Integer> requestQtyMap = requestCombos.stream().collect(Collectors.toMap(
                SelectedCombo::getComboId,
                SelectedCombo::getQuantity));

        List<Combo> foundCombos = comboRepository.findAllById(requestQtyMap.keySet());
        bookingValidator.validateCombo(requestQtyMap.keySet().stream().toList(), foundCombos);
        return foundCombos.stream()
                .collect(Collectors.toMap(
                        combo -> combo,
                        combo -> requestQtyMap.get(combo.getId())
                ));
    }

    /**
     * Build {@link BookingDetailResponse} từ Booking entity đã được eager-load.
     * Đọc totalAmount / finalAmount trực tiếp từ DB (không tính lại) để đảm bảo
     * khớp với giá trị tại thời điểm đặt vé.
     */
    private BookingDetailResponse buildBookingDetailResponse(
            Booking booking,
            List<BookingCombo> bookingCombos) {

        // ── Showtime info ──
        Showtime showtime = booking.getShowtime();
        ShowtimeInfo showtimeInfo = ShowtimeInfo.builder()
                .showtimeId(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .posterUrl(showtime.getMovie().getPosterUrl())
                .build();

        // ── Seats info ──
        List<SeatInfo> seatsInfo = booking.getBookingSeats().stream()
                .map(bs -> SeatInfo.builder()
                        .seatId(bs.getSeat().getSeatId())
                        .rowLabel(bs.getSeat().getSeatRow())
                        .seatNumber(bs.getSeat().getSeatNumber())
                        .seatTypeName(bs.getSeat().getSeatType().getName())
                        .price(bs.getPrice())
                        .build())
                .collect(Collectors.toList());

        // ── Combos info ──
        List<BookingComboInfo> combosInfo = bookingCombos.stream()
                .map(bc -> BookingComboInfo.builder()
                        .comboId(bc.getCombo().getId())
                        .comboName(bc.getCombo().getName())
                        .quantity(bc.getQuantity())
                        .unitPrice(bc.getPrice())
                        .totalPrice(bc.getPrice().multiply(BigDecimal.valueOf(bc.getQuantity())))
                        .build())
                .collect(Collectors.toList());

        // ── Sinh QR Code Base64 từ qrCodeData (chỉ khi booking đã PAID/CHECKED_IN) ──
        String qrCodeBase64 = null;
        if (booking.getQrCodeData() != null && !booking.getQrCodeData().isBlank()) {
            try {
                byte[] qrBytes = QrCodeUtil.generateQrCode(booking.getQrCodeData());
                qrCodeBase64 = Base64.getEncoder().encodeToString(qrBytes);
            } catch (Exception e) {
                log.warn("Could not generate QR code image for booking {}: {}",
                        booking.getBookingCode(), e.getMessage());
            }
        }

        // ── Build response ──
        return BookingDetailResponse.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus())
                .showtime(showtimeInfo)
                .seats(seatsInfo)
                .combos(combosInfo)
                .totalAmount(booking.getTotalAmount())
                .finalAmount(booking.getFinalAmount())
                .qrCodeData(booking.getQrCodeData())
                .qrCodeImageUrl(booking.getQrCodeImageUrl())
                .qrCodeBase64(qrCodeBase64)
                .checkedInAt(booking.getCheckedInAt())
                .checkedInLocation(booking.getCheckedInLocation())
                .loyaltyPointsUsed(booking.getLoyaltyPointsUsed())
                .bookedAt(booking.getCreatedAt())
                .expiresAt(booking.getCreatedAt().plusMinutes(PAYMENT_TIMEOUT_MINUTES))
                .build();
    }
}

