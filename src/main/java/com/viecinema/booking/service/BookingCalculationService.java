package com.viecinema.booking.service;

import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.booking.dto.BookingComboInfo;
import com.viecinema.booking.dto.SelectedCombo;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.booking.dto.PricingContext;
import com.viecinema.booking.dto.request.CalculateBookingRequest;
import com.viecinema.booking.dto.response.CalculateBookingResponse;
import com.viecinema.booking.entity.Combo;
import com.viecinema.booking.repository.ComboRepository;
import com.viecinema.booking.validator.BookingValidator;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import com.viecinema.showtime.entity.Seat;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.showtime.repository.SeatRepository;
import com.viecinema.showtime.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingCalculationService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final ComboRepository comboRepository;

    private final ComboService comboService;
    private final BookingValidator bookingValidator;

    @Transactional
    public CalculateBookingResponse calculateBooking(
            Integer userId,
            CalculateBookingRequest request) {

        log.info("Calculating booking for user: {}, showtime: {}", userId, request.getShowtimeId());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User"));
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("showtime"));
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());

        bookingValidator.validateUser(user);
        bookingValidator.validateShowtime(showtime);
        bookingValidator.validateSeatAvailability(request.getShowtimeId(), request.getSeatIds(), userId);

        Map<Combo, Integer> selectedCombos = resolveCombos(request);

        PricingContext context =
                PricingContext.builder()
                        .user(user)
                        .showtime(showtime)
                        .seats(seats)
                        .selectedCombos(selectedCombos)
                        .promoCode(request.getPromotionCode())
                        .voucherCode("")
                        .useLoyaltyPoints(0)
                        .build();

        PriceBreakdown priceBreakdown = calculatePrice(context);

        List<SeatInfo> seatInfos = new ArrayList<>();
        BigDecimal seatsSubtotal = BigDecimal.ZERO;

        for (Seat seat : seats) {
            BigDecimal seatPrice = showtime.getBasePrice()
                    .multiply(seat.getSeatType().getPriceMultiplier())
                    .setScale(0, RoundingMode.HALF_UP);

            seatsSubtotal = seatsSubtotal.add(seatPrice);

            seatInfos.add(
                    SeatInfo.builder()
                            .seatId(seat.getSeatId())
                            .rowLabel(seat.getSeatRow())
                            .seatNumber(seat.getSeatNumber())
                            .seatTypeName(seat.getSeatType().getName())
                            .priceMultiplier(seat.getSeatType().getPriceMultiplier())
                            .price(seatPrice)
                            .build());
        }

        // Calculate combo price
        List<BookingComboInfo> bookingComboInfos = new ArrayList<>();

        if (request.getCombos() != null && !request.getCombos().isEmpty()) {
            List<Integer> comboIds = request.getCombos().stream()
                    .map(SelectedCombo::getComboId)
                    .collect(Collectors.toList());

            List<Combo> combos = comboService.getCombosByIds(comboIds);
            Map<Integer, Combo> comboMap = combos.stream()
                    .collect(Collectors.toMap(Combo::getId, c -> c));

            for (SelectedCombo item : request.getCombos()) {
                Combo combo = comboMap.get(item.getComboId());
                if (combo == null) {
                    throw new ResourceNotFoundException("Combo doesn't exists: " + item.getComboId());
                }
                bookingComboInfos.add(BookingComboInfo.builder()
                        .comboId(combo.getId())
                        .comboName(combo.getName())
                        .quantity(item.getQuantity())
                        .unitPrice(combo.getPrice())
                        .totalPrice(combo.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build());
            }
        }
        // 8.Build response
        return CalculateBookingResponse.builder()
                .showtime(buildShowtimeInfo(showtime))
                .seats(seatInfos)
                .combos(bookingComboInfos)
                .pricingBreakdown(priceBreakdown)
                .build();
    }

    public PriceBreakdown calculatePrice(PricingContext context) {

        // Calculate ticket price
        Showtime showtime = context.getShowtime();

        List<Seat> seats = context.getSeats();

        BigDecimal ticketsSubtotal = seats.stream()
                .map(seat -> {
                    BigDecimal basePrice = showtime.getBasePrice();
                    BigDecimal multiplier = seat.getSeatType().getPriceMultiplier();
                    return basePrice.multiply(multiplier);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Calculate combo price
        Map<Combo, Integer> selectedCombos = context.getSelectedCombos();
        BigDecimal combosSubtotal = selectedCombos.entrySet().stream()
                .map(entry -> {
                    BigDecimal price = entry.getKey().getPrice();
                    BigDecimal quantity = BigDecimal.valueOf(entry.getValue());
                    return price.multiply(quantity);
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        BigDecimal subtotal = ticketsSubtotal.add(combosSubtotal);

        // Apply discount
        BigDecimal promoDiscount = BigDecimal.ZERO;
        BigDecimal voucherDiscount = BigDecimal.ZERO;
        BigDecimal loyaltyDiscount = BigDecimal.ZERO;
        BigDecimal membershipDiscount = BigDecimal.ZERO;
        int pointsEarned = 0;

        BigDecimal totalDiscount = promoDiscount
                .add(voucherDiscount)
                .add(loyaltyDiscount)
                .add(membershipDiscount);

        BigDecimal finalAmount = subtotal.subtract(totalDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

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


    private Map<Combo, Integer> resolveCombos(CalculateBookingRequest request) {
        List<SelectedCombo> selectedCombos = request.getCombos();
        if (selectedCombos == null) {
            return new java.util.HashMap<>();
        }
        Map<Integer, Integer> quantityMap = selectedCombos.stream().collect(Collectors.toMap(
                SelectedCombo::getComboId,
                SelectedCombo::getQuantity));
        List<Combo> combos = comboRepository.findAllById(quantityMap.keySet());
        bookingValidator.validateCombo(quantityMap.keySet().stream().toList(), combos);
        Map<Combo, Integer> combosWithQuantity = combos.stream().collect(Collectors.toMap(
                combo -> combo,
                combo -> quantityMap.get(combo.getId())));
        return combosWithQuantity;
    }

    private ShowtimeInfo buildShowtimeInfo(Showtime showtime) {
        return ShowtimeInfo.builder()
                .showtimeId(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .roomName(showtime.getRoom().getName())
                .startTime(showtime.getStartTime())
                .basePrice(showtime.getBasePrice())
                .build();
    }
}
