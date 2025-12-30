package com.viecinema.user.service;

import com.viecinema.booking.dto.ComboInfo;
import com.viecinema.booking.entity.Booking;
import com.viecinema.booking.entity.BookingCombo;
import com.viecinema.booking.entity.BookingSeat;
import com.viecinema.booking.repository.BookingComboRepository;
import com.viecinema.booking.repository.BookingRepository;
import com.viecinema.booking.repository.BookingSeatRepository;
import com.viecinema.movie.entity.Genre;
import com.viecinema.movie.entity.Movie;
import com.viecinema.movie.entity.MovieGenre;
import com.viecinema.movie.repository.MovieGenresRepository;
import com.viecinema.payment.entity.Payment;
import com.viecinema.payment.repository.PaymentRepository;
import com.viecinema.showtime.dto.CinemaInfo;
import com.viecinema.showtime.dto.MovieInfo;
import com.viecinema.showtime.dto.SeatInfo;
import com.viecinema.showtime.dto.ShowtimeInfo;
import com.viecinema.showtime.entity.Showtime;
import com.viecinema.user.dto.PaymentInfo;
import com.viecinema.user.dto.UserBookingDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserBookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingComboRepository bookingComboRepository;
    private final PaymentRepository paymentRepository;
    private final MovieGenresRepository movieGenresRepository;

    /**
     * Lấy tất cả booking của user
     *
     * @param userId ID của user
     * @return List<UserBookingDto>
     */
    @Transactional(readOnly = true)
    public List<UserBookingDto> getAllUserBookings(Integer userId) {
        log.info("Fetching all bookings for user ID: {}", userId);

        // 1. Lấy tất cả booking với eager loading
        List<Booking> bookings = bookingRepository.findAllByUserIdWithDetails(userId);

        if (bookings.isEmpty()) {
            log.info("No bookings found for user ID: {}", userId);
            return Collections.emptyList();
        }

        // 2. Lấy booking IDs
        List<Integer> bookingIds = bookings.stream()
                .map(Booking::getId)
                .collect(Collectors.toList());

        // 3. Lấy tất cả combos và payments (batch loading)
        Map<Integer, List<BookingCombo>> combosMap = loadBookingCombos(bookingIds);
        Map<Integer, Payment> paymentsMap = loadPayments(bookingIds);

        // 4. Lấy genres của tất cả movies
        Set<Integer> movieIds = bookings.stream()
                .map(b -> b.getShowtime().getMovie().getMovieId())
                .collect(Collectors.toSet());
        Map<Integer, List<String>> movieGenresMap = loadMovieGenre(new ArrayList<>(movieIds));

        // 5. Convert sang DTO
        LocalDateTime now = LocalDateTime.now();

        return bookings.stream()
                .map(booking -> convertToDto(
                        booking,
                        combosMap.get(booking.getId()),
                        paymentsMap.get(booking.getId()),
                        movieGenresMap.get(booking.getShowtime().getMovie().getMovieId()),
                        now
                ))
                .collect(Collectors.toList());
    }

    /**
     * Lấy booking theo trạng thái
     */
    @Transactional(readOnly = true)
    public List<UserBookingDto> getUserBookingsByStatus(Integer userId, String status) {
        log.info("Fetching bookings for user ID: {} with status: {}", userId, status);

        List<Booking> bookings = bookingRepository.findByUserIdAndStatus(userId, status);

        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        return convertBookingsToDto(bookings);
    }

    /**
     * Lấy booking sắp tới
     */
    @Transactional(readOnly = true)
    public List<UserBookingDto> getUpcomingBookings(Integer userId) {
        log.info("Fetching upcoming bookings for user ID: {}", userId);

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findUpcomingBookings(userId, now);

        return convertBookingsToDto(bookings);
    }

    /**
     * Lấy booking đã qua
     */
    @Transactional(readOnly = true)
    public List<UserBookingDto> getPastBookings(Integer userId) {
        log.info("Fetching past bookings for user ID:  {}", userId);

        LocalDateTime now = LocalDateTime.now();
        List<Booking> bookings = bookingRepository.findPastBookings(userId, now);

        return convertBookingsToDto(bookings);
    }

    /**
     * Helper:  Convert list bookings sang DTO
     */
    private List<UserBookingDto> convertBookingsToDto(List<Booking> bookings) {
        if (bookings.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> bookingIds = bookings. stream()
                .map(Booking::getId)
                .collect(Collectors.toList());

        Map<Integer, List<BookingCombo>> combosMap = loadBookingCombos(bookingIds);
        Map<Integer, Payment> paymentsMap = loadPayments(bookingIds);

        Set<Integer> movieIds = bookings.stream()
                .map(b -> b.getShowtime().getMovie().getMovieId())
                .collect(Collectors. toSet());
        Map<Integer, List<String>> movieGenresMap = loadMovieGenre(new ArrayList<>(movieIds));

        LocalDateTime now = LocalDateTime.now();

        return bookings.stream()
                .map(booking -> convertToDto(
                        booking,
                        combosMap.get(booking.getId()),
                        paymentsMap. get(booking.getId()),
                        movieGenresMap.get(booking.getShowtime().getMovie().getMovieId()),
                        now
                ))
                .collect(Collectors. toList());
    }

    /**
     * Helper:  Load booking combos (batch)
     */
    private Map<Integer, List<BookingCombo>> loadBookingCombos(List<Integer> bookingIds) {
        List<BookingCombo> allCombos = bookingComboRepository.findByBookingIdsWithCombo(bookingIds);

        return allCombos.stream()
                .collect(Collectors.groupingBy(
                        bc -> bc.getBooking().getId()
                ));
    }

    /**
     * Helper: Load payments (batch)
     */
    private Map<Integer, Payment> loadPayments(List<Integer> bookingIds) {
        List<Payment> payments = paymentRepository.findByBookingIds(bookingIds);

        return payments. stream()
                .collect(Collectors.toMap(
                        p -> p.getBooking().getId(),
                        p -> p
                ));
    }

    /**
     * Helper: Load movie genres (batch)
     */
    private Map<Integer, List<String>> loadMovieGenre(List<Integer> movieIds) {
        List<MovieGenre> movieGenresList = movieGenresRepository.findByMovieIds(movieIds);

        return movieGenresList.stream()
                .collect(Collectors.groupingBy(
                        mg -> mg.getMovie().getMovieId(),
                        Collectors.mapping(
                                mg -> mg. getGenre().getName(),
                                Collectors.toList()
                        )
                ));
    }

    /**
     * Convert Booking entity sang DTO
     */
    private UserBookingDto convertToDto(Booking booking,
                                        List<BookingCombo> combos,
                                        Payment payment,
                                        List<String> genres,
                                        LocalDateTime now) {

        Showtime showtime = booking.getShowtime();
        Movie movie = showtime.getMovie();

        // Build ShowtimeInfo
           ShowtimeInfo showtimeInfo =   ShowtimeInfo.builder()
                .showtimeId(showtime.getId())
                .startTime(showtime.getStartTime())
                .endTime(showtime.getEndTime())
                .basePrice(showtime.getBasePrice())
                .build();

        // Build MovieInfo
           MovieInfo movieInfo =   MovieInfo.builder()
                .movieId(movie.getMovieId())
                .title(movie.getTitle())
                .posterUrl(movie.getPosterUrl())
                .duration(movie.getDuration())
                .ageRating(movie.getAgeRating())
                .genres(genres != null ? genres : Collections.emptyList())
                .build();

        // Build CinemaInfo
          CinemaInfo cinemaInfo =   CinemaInfo.builder()
                .cinemaId(showtime.getRoom().getCinema().getId())
                .name(showtime. getRoom().getCinema().getName())
                .address(showtime.getRoom().getCinema().getAddress())
                .city(showtime.getRoom().getCinema().getCity())
                .build();

        // Build Seats
        List<SeatInfo> seats = bookingSeatRepository.findAllByBooking(booking).stream()
                .map(this::convertSeatToDto)
                .sorted(Comparator.comparing(SeatInfo::getSeatLabel))
                .collect(Collectors.toList());

        // Build Combos
        List<ComboInfo> comboInfos = Collections.emptyList();
        if (combos != null && !combos.isEmpty()) {
            comboInfos = combos.stream()
                    .map(this::convertComboToDto)
                    .collect(Collectors. toList());
        }

        // Build PaymentInfo
          PaymentInfo paymentInfo = null;
        if (payment != null) {
            paymentInfo =   PaymentInfo.builder()
                    .paymentId(payment.getPaymentId())
                    .method(payment.getMethod())
                    .status(payment.getStatus().name())
                    .transactionId(payment.getTransactionId())
                    .transactionTime(payment.getTransactionTime())
                    .build();
        }

        // Calculate helper flags
        boolean isUpcoming = "paid".equals(booking.getStatus()) && showtime.getStartTime().isAfter(now);
        boolean isPast = showtime.getEndTime().isBefore(now);
        boolean canCancel = canCancelBooking(booking, showtime, now);
        boolean canCheckIn = canCheckInBooking(booking, showtime, now);

        // Build final DTO
        return   UserBookingDto.builder()
                .bookingId(booking.getId())
                .bookingCode(booking.getBookingCode())
                .status(booking.getStatus().name())
                .totalAmount(booking.getTotalAmount())
                .finalAmount(booking.getFinalAmount())
                .createdAt(booking.getCreatedAt())
                .qrCodeImageUrl(booking.getQrCodeImageUrl())
                .isCheckedIn(booking.getCheckedInAt() != null)
                .checkedInAt(booking.getCheckedInAt())
                .showtime(showtimeInfo)
                .movie(movieInfo)
                .cinema(cinemaInfo)
                .seats(seats)
                .combos(comboInfos)
                .payment(paymentInfo)
                .canCancel(canCancel)
                .canCheckIn(canCheckIn)
                .isUpcoming(isUpcoming)
                .isPast(isPast)
                .build();
    }

    /**
     * Convert BookingSeat sang SeatInfo DTO
     */
    private   SeatInfo convertSeatToDto(BookingSeat bookingSeat) {
        return   SeatInfo. builder()
                .seatId(bookingSeat.getSeat().getSeatId())
                .rowLabel(bookingSeat. getSeat().getSeatRow())
                .seatNumber(bookingSeat.getSeat().getSeatNumber())
                .seatTypeName(bookingSeat. getSeat().getSeatType().getName())
                .price(bookingSeat.getPrice())
                .seatLabel(bookingSeat.getSeat().getSeatRow() + bookingSeat.getSeat().getSeatNumber())
                .build();
    }

    /**
     * Convert BookingCombo sang ComboInfo DTO
     */
    private    ComboInfo convertComboToDto(BookingCombo bookingCombo) {
        return   ComboInfo.builder()
                .comboId(bookingCombo.getCombo().getId())
                .comboName(bookingCombo. getCombo().getName())
                .quantity(bookingCombo. getQuantity())
                .unitPrice(bookingCombo.getPrice())
                .totalPrice(bookingCombo.getPrice().multiply(new java.math.BigDecimal(bookingCombo.getQuantity())))
                .build();
    }

    /**
     * Lấy thứ trong tuần bằng tiếng Việt
     */
    private String getDayOfWeekVietnamese(LocalDateTime dateTime) {
        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        switch (dayOfWeek) {
            case MONDAY:  return "Thứ 2";
            case TUESDAY: return "Thứ 3";
            case WEDNESDAY: return "Thứ 4";
            case THURSDAY: return "Thứ 5";
            case FRIDAY: return "Thứ 6";
            case SATURDAY: return "Thứ 7";
            case SUNDAY: return "Chủ Nhật";
            default: return "";
        }
    }

    /**
     * Xác định khung giờ chiếu
     */
    private String getTimeSlot(LocalDateTime startTime) {
        int hour = startTime.getHour();
        if (hour >= 6 && hour < 12) {
            return "Suất chiếu sáng";
        } else if (hour >= 12 && hour < 18) {
            return "Suất chiếu chiều";
        } else {
            return "Suất chiếu tối";
        }
    }

    /**
     * Kiểm tra có thể hủy booking không
     * Logic: Chỉ hủy được nếu:
     * - Status = paid
     * - Chưa check-in
     * - Còn ít nhất 2 giờ trước suất chiếu
     */
    private boolean canCancelBooking(Booking booking, Showtime showtime, LocalDateTime now) {
        if (!"paid".equals(booking.getStatus())) {
            return false;
        }

        if (booking.getCheckedInAt() != null) {
            return false;
        }

        // Phải còn ít nhất 2 giờ
        LocalDateTime cancelDeadline = showtime.getStartTime().minusHours(2);
        return now.isBefore(cancelDeadline);
    }

    /**
     * Kiểm tra có thể check-in không
     * Logic:
     * - Status = paid
     * - Chưa check-in
     * - Trong khoảng 30 phút trước đến khi suất chiếu bắt đầu
     */
    private boolean canCheckInBooking(Booking booking, Showtime showtime, LocalDateTime now) {
        if (!"paid". equals(booking.getStatus())) {
            return false;
        }

        if (booking.getCheckedInAt() != null) {
            return false;
        }

        // Cho phép check-in từ 30 phút trước
        LocalDateTime checkInStartTime = showtime.getStartTime().minusMinutes(30);
        LocalDateTime checkInEndTime = showtime.getStartTime();

        return now.isAfter(checkInStartTime) && now.isBefore(checkInEndTime);
    }
}
