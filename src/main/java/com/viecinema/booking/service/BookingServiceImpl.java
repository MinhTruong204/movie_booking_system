package com.viecinema.booking.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.dto.ComboInfo;
import com.viecinema.booking.dto.ComboItem;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.booking.dto.request.BookingRequest;
import com.viecinema.booking.dto.request.CalculateBookingRequest;
import com.viecinema.booking.dto.response.BookingResponse;
import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.BookingCombo;
import com.viecinema.booking.entity.BookingSeat;
import com.viecinema.booking.entity.Combo;
import com.viecinema.booking.repository.BookingComboRepository;
import com.viecinema.booking.repository.BookingRepository;
import com.viecinema.booking.repository.BookingSeatRepository;
import com.viecinema.booking.repository.ComboRepository;
import com.viecinema.common.enums.BookingStatus;
import com.viecinema.common.enums.SeatStatusType;
import com.viecinema.common.exception.SpecificBusinessException;
import com.viecinema.common.exception.ResourceNotFoundException;
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
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final SeatStatusRepository seatStatusRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingComboRepository bookingComboRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ComboRepository comboRepository;
    private final UserRepository userRepository;

    private final PromotionService promotionService;
    private final VoucherService voucherService;
    private final LoyaltyService loyaltyService;
//    private final SeatLockingService seatLockingService;

    private static final int PAYMENT_TIMEOUT_MINUTES = 10;
    private static final int POINTS_PER_1000_VND = 1; // 1000đ = 1 điểm

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public BookingResponse createBooking(Integer userId, BookingRequest request) {
        log.info("Creating booking for user {} - showtime {}", userId, request.getShowtimeId());

        // Validate user
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        if (!user.getIsActive()) {
            throw new SpecificBusinessException("The account has been locked.");
        }

        // Validate showtime
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("Showtime"));

        validateShowtime(showtime);

        // Validate và lock seat (CRITICAL SECTION)
        validateSeatAvailability(request.getShowtimeId(), request.getSeatIds(), userId);
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());

        if (seats.size() != request.getSeatIds().size()) {
            throw new SpecificBusinessException("Some seats don't exist.");
        }

        // Lock ghế để tránh race condition
//        try {
//            seatLockingService.lockSeats(request.getShowtimeId(), request.getSeatIds(), userId);
//        } catch (Exception e) {
//            log.error("Failed to lock seats", e);
//            throw new SpecificBusinessException("Không thể đặt ghế, vui lòng thử lại");
//        }

        // Validate combos
        List<Combo> combos = validateAndGetCombos(request.getCombos());

        // Calculate price
        PriceBreakdown priceBreakdown = calculatePrice(
                userId,
                request.getShowtimeId(),
                request.getSeatIds(),
                request.getCombos(),
                request.getPromoCode(),
                request.getVoucherCode(),
                request.getLoyaltyPointsToUse()
        );

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .showtime(showtime)
                .bookingCode(generateBookingCode())
                .status(BookingStatus.PENDING)
                .totalAmount(priceBreakdown.getSubtotal())
                .finalAmount(priceBreakdown.getFinalAmount())
                .build();

        booking = bookingRepository.save(booking);

        // Create booking info
        List<BookingSeat> bookingSeats = saveBookingSeats(booking, seats, showtime);

        // Create combo infos
        List<BookingCombo> bookingCombos = saveBookingCombos(booking, combos, request.getCombos());

        // Update seat status
        updateSeatStatus(request.getShowtimeId(), request.getSeatIds(), SeatStatusType.BOOKED, booking);

        // Apply promotions and vouchers
        applyPromotionsAndVouchers(booking, request.getPromoCode(), request.getVoucherCode(), priceBreakdown);

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

    @Override
    public void validateSeatAvailability(Integer showtimeId, List<Integer> seatIds, Integer userId) {
        // Get seat statuses
        List<SeatStatus> seatStatuses = seatStatusRepository
                .findByShowtimeAndSeatsWithLock(showtimeId, seatIds);

        // Check seat statuses
        if (seatStatuses.size() != seatIds.size()) {
            Set<Integer> existingSeatIds = seatStatuses.stream()
                    .map(ss -> ss.getSeat().getSeatId())
                    .collect(Collectors.toSet());
            // Get missing seat ids, which one is not in existingSeatIds
            List<Integer> missingSeatIds = seatIds.stream()
                    .filter(id -> !existingSeatIds.contains(id))
                    .collect(Collectors.toList());

            List<Seat> missingSeats = seatRepository.findAllById(missingSeatIds);
            Showtime showtime = showtimeRepository.findById(showtimeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

            // Save a new status for missing seats
            for (Seat seat : missingSeats) {
                SeatStatus newStatus = SeatStatus.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(SeatStatusType.AVAILABLE)
                        .version(0)
                        .build();
                seatStatusRepository.save(newStatus);
            }

            // Get all seat statuses again
            seatStatuses = seatStatusRepository
                    .findByShowtimeAndSeatsWithLock(showtimeId, seatIds);
        }

        // Check status
        LocalDateTime now = LocalDateTime.now();
        for (SeatStatus status : seatStatuses) {
            if (SeatStatusType.BOOKED.equals(status.getStatus())) {
                throw new SpecificBusinessException(
                        String.format("Ghế %s%d đã được đặt",
                                status.getSeat().getSeatRow(),
                                status.getSeat().getSeatNumber())
                );
            }

            if (SeatStatusType.HELD.equals(status.getStatus())) {
                // Kiểm tra xem ghế có hết hạn giữ chưa
                if (status.getHeldUntil() != null && status.getHeldUntil().isAfter(now)) {
                    // Nếu ghế đang được giữ bởi người khác, báo lỗi
                    if (status.getHeldByUser() != null && !status.getHeldByUser().getId().equals(userId)) {
                        throw new SpecificBusinessException(
                                String.format("Ghế %s%d đang được giữ bởi người khác",
                                        status.getSeat().getSeatRow(),
                                        status.getSeat().getSeatNumber())
                        );
                    }
                }
            }
        }
    }

    @Override
    public PriceBreakdown calculatePrice(
            Integer userId,
            Integer showtimeId,
            List<Integer> seatIds,
            List<ComboItem> comboItems,
            String promoCode,
            String voucherCode,
            Integer loyaltyPointsToUse) {

        // 1.Tính giá vé
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Showtime not found"));

        List<Seat> seats = seatRepository.findAllById(seatIds);

        BigDecimal ticketsSubtotal = seats.stream()
                .map(seat -> {
                    BigDecimal basePrice = showtime.getBasePrice();
                    BigDecimal multiplier = seat.getSeatType().getPriceMultiplier();
                    return basePrice.multiply(multiplier);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2.Tính giá combo
        BigDecimal combosSubtotal = BigDecimal.ZERO;
        if (comboItems != null && !comboItems.isEmpty()) {
            List<Integer> comboIds = comboItems.stream()
                    .map(ComboItem::getComboId)
                    .collect(Collectors.toList());

            List<Combo> combos = comboRepository.findAllById(comboIds);
            Map<Integer, Combo> comboMap = combos.stream()
                    .collect(Collectors.toMap(Combo::getId, c -> c));

            combosSubtotal = comboItems.stream()
                    .map(item -> {
                        Combo combo = comboMap.get(item.getComboId());
                        if (combo == null) {
                            throw new ResourceNotFoundException("Combo not found:  " + item.getComboId());
                        }
                        return combo.getPrice()
                                .multiply(BigDecimal.valueOf(item.getQuantity()));
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        BigDecimal subtotal = ticketsSubtotal.add(combosSubtotal);

        // 3.Áp dụng giảm giá
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal promoDiscount = BigDecimal.ZERO;
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        BigDecimal membershipDiscount = BigDecimal.ZERO;

        // 3.1 Promotion code
        if (promoCode != null && !promoCode.isEmpty()) {
            promoDiscount = promotionService.calculateDiscount(
                    promoCode, userId, subtotal, showtimeId
            );
        }

        // 3.2 Voucher
        if (voucherCode != null && !voucherCode.isEmpty()) {
            voucherDiscount = voucherService.calculateDiscount(
                    voucherCode, userId, subtotal
            );
        }

        // 3.3 Loyalty points (1000 points = 10,000 VND)
        if (loyaltyPointsToUse != null && loyaltyPointsToUse > 0) {
            int userPoints = user.getLoyaltyPoints();
            if (loyaltyPointsToUse > userPoints) {
                throw new SpecificBusinessException("Không đủ điểm tích lũy");
            }
            loyaltyDiscount = BigDecimal.valueOf(loyaltyPointsToUse * 10);
        }

        // 3.4 Membership discount (áp dụng trên tickets only)
        BigDecimal membershipPercent = user.getMembershipTier().getDiscountPercent();
        if (membershipPercent.compareTo(BigDecimal.ZERO) > 0) {
            membershipDiscount = ticketsSubtotal
                    .multiply(membershipPercent)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        // 4.Tổng giảm giá
        BigDecimal totalDiscount = promoDiscount
                .add(voucherDiscount)
                .add(loyaltyDiscount)
                .add(membershipDiscount);

        // 5.Số tiền cuối cùng (không được âm)
        BigDecimal finalAmount = subtotal.subtract(totalDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 6.Tính điểm tích lũy nhận được (1000đ = 1 điểm)
        int pointsEarned = finalAmount
                .divide(BigDecimal.valueOf(1000), 0, RoundingMode.DOWN)
                .intValue();

        return PriceBreakdown.builder()
                .ticketsSubtotal(ticketsSubtotal)
                .combosSubtotal(combosSubtotal)
                .subtotal(subtotal)
                .promoDiscount(promoDiscount)
                .voucherDiscount(voucherDiscount)
                .loyaltyDiscount(loyaltyDiscount)
                .membershipDiscount(membershipDiscount)
                .totalDiscount(totalDiscount)
                .finalAmount(finalAmount)
                .pointsEarned(pointsEarned)
                .build();
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void validateShowtime(Showtime showtime) {
        if (! showtime.getIsActive()) {
            throw new SpecificBusinessException("Suất chiếu không khả dụng");
        }

        if (showtime.getStartTime().isBefore(LocalDateTime.now())) {
            throw new SpecificBusinessException("Suất chiếu đã bắt đầu");
        }

        // Không cho đặt vé trước giờ chiếu < 15 phút
        if (showtime.getStartTime().minusMinutes(15).isBefore(LocalDateTime.now())) {
            throw new SpecificBusinessException("Không thể đặt vé cho suất chiếu sắp diễn ra");
        }
    }

    private List<Combo> validateAndGetCombos(List<ComboItem> comboItems) {
        if (comboItems == null || comboItems.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> comboIds = comboItems.stream()
                .map(ComboItem:: getComboId)
                .collect(Collectors.toList());

        List<Combo> combos = comboRepository.findAllById(comboIds);

        if (combos.size() != comboIds.size()) {
            throw new SpecificBusinessException("Một số combo không tồn tại");
        }

        for (Combo combo : combos) {
            if (! combo.getIsActive()) {
                throw new SpecificBusinessException("Combo " + combo.getName() + " không khả dụng");
            }
        }

        return combos;
    }

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
            List<Combo> combos,
            List<ComboItem> comboItems) {

        if (combos.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Integer, Combo> comboMap = combos.stream()
                .collect(Collectors.toMap(Combo::getId, c -> c));

        List<BookingCombo> bookingCombos = new ArrayList<>();

        for (ComboItem item : comboItems) {
            Combo combo = comboMap.get(item.getComboId());

            BookingCombo bookingCombo = BookingCombo.builder()
                    .booking(booking)
                    .combo(combo)
                    .quantity(item.getQuantity())
                    .price(combo.getPrice())
                    .build();

            bookingCombos.add(bookingComboRepository.save(bookingCombo));
        }

        return bookingCombos;
    }

    private void updateSeatStatus(Integer showtimeId, List<Integer> seatIds, SeatStatusType status, Booking booking) {
        List<SeatStatus> seatStatuses = seatStatusRepository
                .findByShowtimeAndSeatsWithLock(showtimeId, seatIds);

        for (SeatStatus seatStatus : seatStatuses) {
            seatStatus.setStatus(status);
            seatStatus.setHeldByUser(null);
            seatStatus.setHeldUntil(null);
            seatStatus.setVersion(seatStatus.getVersion() + 1);
            seatStatusRepository.save(seatStatus);
        }
    }

    private void applyPromotionsAndVouchers(
            Booking booking,
            String promoCode,
            String voucherCode,
            PriceBreakdown priceBreakdown) {

        // Lưu promotion đã dùng
        if (promoCode != null && !promoCode.isEmpty()) {
            promotionService.applyPromotion(
                    promoCode,
                    booking.getUser().getId(),
                    booking.getId(),
                    priceBreakdown.getPromoDiscount()
            );
        }

        // Lưu voucher đã dùng
        if (voucherCode != null && !voucherCode.isEmpty()) {
            voucherService.applyVoucher(
                    voucherCode,
                    booking.getUser().getId(),
                    booking.getId(),
                    priceBreakdown.getVoucherDiscount()
            );
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
        List<ComboInfo> combosInfo = bookingCombos.stream()
                .map(bc -> ComboInfo.builder()
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
}
