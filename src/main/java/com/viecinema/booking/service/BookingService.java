package com.viecinema.booking.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.dto.BookingComboInfo;
import com.viecinema.booking.dto.SelectedCombo;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.booking.dto.PricingContext;
import com.viecinema.booking.dto.request.BookingRequest;
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
import com.viecinema.common.enums.SeatStatusType;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
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
                .useLoyaltyPoints(request.getUseLoyaltyPoints() ? request.getLoyaltyPointsToUse() : 0)
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
        updateSeatStatus(request.getShowtimeId(), request.getSeatIds(), booking);

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
            seatStatus.setVersion(seatStatus.getVersion() + 1);
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
}
