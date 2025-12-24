package com.viecinema.booking.service;

import com.viecinema.booking.dto.ComboInfo;
import com.viecinema.booking.dto.ComboItem;
import com.viecinema.booking.dto.PriceBreakdown;
import com.viecinema.booking.dto.PromotionInfo;
import com.viecinema.booking.dto.request.CalculateBookingRequest;
import com.viecinema.booking.dto.response.CalculateBookingResponse;
import com.viecinema.booking.entity.Combo;
import com.viecinema.booking.entity.Promotion;
import com.viecinema.booking.entity.UserPromotionUsage;
import com.viecinema.booking.repository.PromotionRepository;
import com.viecinema.booking.repository.UserPromotionUsageRepository;
import com.viecinema.common.exception.SpecificBusinessException;
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
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BookingCalculationService {

    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final ComboService comboService;
    private final PromotionRepository promotionRepository;
    private final UserPromotionUsageRepository userPromotionUsageRepository;

    /**
     * Tính toán chi tiết giá đặt vé
     */
    public CalculateBookingResponse calculateBooking(
            Integer userId,
            CalculateBookingRequest request) {

        log.info("Calculating booking for user: {}, showtime: {}", userId, request. getShowtimeId());

        // 1. Validate và lấy thông tin showtime
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> new ResourceNotFoundException("showtime"));

        if (!showtime.getIsActive()) {
            throw new ResourceNotFoundException("showtime");
        }

        // 2. Tính giá ghế
        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.size() != request.getSeatIds(). size()) {
            throw new ResourceNotFoundException("Seats");
        }

        List<SeatInfo> seatInfos = new ArrayList<>();
        BigDecimal seatsSubtotal = BigDecimal.ZERO;

        for (Seat seat : seats) {
            BigDecimal seatPrice = showtime.getBasePrice()
                    .multiply(seat.getSeatType().getPriceMultiplier())
                    .setScale(0, RoundingMode.HALF_UP);

            seatsSubtotal = seatsSubtotal.add(seatPrice);

            seatInfos.add(SeatInfo.builder()
                    .seatId(seat.getSeatId())
                    .rowLabel(seat.getSeatRow())
                    .seatNumber(seat.getSeatNumber())
                    .seatTypeName(seat.getSeatType().getName())
                    .priceMultiplier(seat.getSeatType().getPriceMultiplier())
                    .price(seatPrice)
                    .build());
        }

        // 3. Tính giá combo
        List<ComboInfo> comboInfos = new ArrayList<>();
        BigDecimal combosSubtotal = BigDecimal.ZERO;

        if (request.getCombos() != null && !request.getCombos().isEmpty()) {
            List<Integer> comboIds = request.getCombos().stream()
                    .map(ComboItem::getComboId)
                    .collect(Collectors. toList());

            List<Combo> combos = comboService.getCombosByIds(comboIds);
            Map<Integer, Combo> comboMap = combos.stream()
                    .collect(Collectors. toMap(Combo::getId, c -> c));

            for (ComboItem item : request.getCombos()) {
                Combo combo = comboMap.get(item.getComboId());
                if (combo == null) {
                    throw new ResourceNotFoundException("Combo doesn't exists: " + item.getComboId());
                }

                BigDecimal comboTotal = combo.getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity()));
                combosSubtotal = combosSubtotal.add(comboTotal);

                comboInfos.add(ComboInfo.builder()
                        .comboId(combo.getId())
                        .comboName(combo.getName())
                        .quantity(item.getQuantity())
                        .unitPrice(combo.getPrice())
                        .totalPrice(comboTotal)
                        .build());
            }
        }

        // 4. Tính subtotal
        BigDecimal subtotal = seatsSubtotal.add(combosSubtotal);

        // 5.  Áp dụng khuyến mãi (nếu có)
        PromotionInfo promotionInfo = null;
        BigDecimal totalDiscount = BigDecimal.ZERO;

        if (request.getPromotionCode() != null && !request.getPromotionCode().isBlank()) {
            Promotion promotion = validateAndGetPromotion(
                    request.getPromotionCode(),
                    userId,
                    showtime,
                    subtotal
            );

            BigDecimal discountAmount = calculateDiscount(promotion, subtotal);
            totalDiscount = discountAmount;

            promotionInfo = PromotionInfo. builder()
                    .code(promotion.getCode())
                    . description(promotion.getDescription())
                    .discountType(promotion.getDiscountType(). name())
                    .discountValue(promotion.getDiscountValue())
                    .discountAmount(discountAmount)
                    .build();
        }

        // 6. Tính final amount
        BigDecimal finalAmount = subtotal.subtract(totalDiscount);
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 7. Tính điểm tích lũy (1 điểm = 10,000 VNĐ)
        Integer loyaltyPoints = finalAmount.divide(BigDecimal.valueOf(10000), 0, RoundingMode.DOWN).intValue();

        // 8. Build response
        return CalculateBookingResponse.builder()
                .showtime(buildShowtimeInfo(showtime))
                .seats(seatInfos)
                .combos(comboInfos)
                .pricingBreakdown(PriceBreakdown.builder()
                        .ticketsSubtotal(seatsSubtotal)
                        .combosSubtotal(combosSubtotal)
                        .subtotal(subtotal)
                        .promoDiscount(promotionInfo != null ? promotionInfo.getDiscountAmount() : BigDecimal.ZERO)
                        .totalDiscount(totalDiscount)
                        .finalAmount(finalAmount)
                        .pointsEarned(loyaltyPoints)
                        .build())
                .build();
    }

    /**
     * Validate promotion và kiểm tra điều kiện
     */
    private Promotion validateAndGetPromotion(
            String code,
            Integer userId,
            Showtime showtime,
            BigDecimal subtotal) {

        Promotion promotion = promotionRepository.findByCodeAndIsActiveTrue(code)
                .orElseThrow(() -> new SpecificBusinessException("The promotional code does not exist."));

        // Kiểm tra thời gian
        if (! promotion.isValid()) {
            throw new SpecificBusinessException("Promotion code has expired or is not active");
        }

        // Kiểm tra giá trị đơn hàng tối thiểu
        if (subtotal.compareTo(promotion. getMinOrderValue()) < 0) {
            throw new SpecificBusinessException(
                    String.format("Orders must reach a minimum value of %,. 0f VNĐ to apply this code.",
                            promotion.getMinOrderValue())
            );
        }

        // Kiểm tra phim áp dụng
        if (! promotion.isApplicableForMovie(showtime.getMovie().getMovieId())) {
            throw new SpecificBusinessException("The promotional code does not apply to this movie.");
        }

        // Kiểm tra ngày áp dụng
        DayOfWeek currentDay = LocalDateTime.now().getDayOfWeek();
        if (!promotion. isApplicableForDay(currentDay)) {
            throw new SpecificBusinessException("The promotional code does not apply to today.");
        }

        // Kiểm tra số lần sử dụng của user
        UserPromotionUsage usage = userPromotionUsageRepository
                .findByUserIdAndPromoId(userId, promotion.getId())
                .orElse(null);

        if (usage != null && usage.getUsageCount() >= promotion.getMaxUsagePerUser()) {
            throw new SpecificBusinessException("The promotional code has been used too many times.");
        }

        return promotion;
    }

    /**
     * Tính số tiền giảm giá
     */
    private BigDecimal calculateDiscount(Promotion promotion, BigDecimal subtotal) {
        BigDecimal discount;

        if (promotion.getDiscountType() == Promotion.DiscountType. PERCENT) {
            discount = subtotal.multiply(promotion.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);

            // Giới hạn bởi max_discount
            if (promotion.getMaxDiscount() != null
                    && discount.compareTo(promotion.getMaxDiscount()) > 0) {
                discount = promotion.getMaxDiscount();
            }
        } else {
            discount = promotion.getDiscountValue();
        }

        // Discount không được lớn hơn subtotal
        if (discount.compareTo(subtotal) > 0) {
            discount = subtotal;
        }

        return discount;
    }

    private ShowtimeInfo buildShowtimeInfo(Showtime showtime) {
        return ShowtimeInfo.builder()
                .showtimeId(showtime.getId())
                .movieTitle(showtime.getMovie().getTitle())
                .cinemaName(showtime.getRoom().getCinema().getName())
                .roomName(showtime.getRoom(). getName())
                .startTime(showtime.getStartTime())
                .basePrice(showtime. getBasePrice())
                .build();
    }
}
