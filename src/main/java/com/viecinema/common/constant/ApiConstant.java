package com.viecinema.common.constant;

public final class ApiConstant {
//  Root API
    public static final String AUTH_PATH = "/api/auth";
    public static final String MOVIE_PATH = "/api/movies";
    public static final String SHOWTIMES_PATH = "/api/showtimes";
    public static final String GENRE_PATH = "/api/genres";
    public static final String BOOKING_PATH = "/api/bookings";
    public static final String COMBO_PATH = "/api/combos";
    public static final String PROMOTION_PATH = "/api/promotions";
    public static final String PAYMENT_PATH = "/api/payments";
    public static final String VNPAY_IPN_PATH = "/vnpay/ipn";
    public static final String USER_PATH = "/api/users";




//  Detail API
    public static final String LOGIN_PATH = "/login";
    public static final String REGISTER_PATH = "/register";
    public static final String MOVIE_NOW_SHOWING_PATH  = "/now-showing";
    public static final String MOVIE_COMING_SOON_PATH = "/coming-soon";
    public static final String MOVIE_DETAIL_PATH = "/{movieId}";
    public static final String SHOWTIMES_DETAIL_PATH = "/{showtimeId}";
    public static final String SHOWTIMES_SEATMAP_PATH = "/{showtimeId}/seatmap";
    public static final String SHOWTIMES_BY_CINEMA_PATH = "/by-cinema/{cinemaId}";
    public static final String SHOWTIMES_BY_MOVIE_PATH = "/by-movie/{movieId}";
    public static final String GENRE_DETAIL_PATH = "/{id}";
    public static final String HOLD_SEATS_PATH = "/hold-seats";
    public static final String RELEASE_SEATS_PATH = "/release-seats";
    public static final String RELEASE_SEAT_PATH = "/release-seat";
    public static final String CAlCULATE_PATH = "/calculate";
    public static final String CREATE_BOOKING_PATH = "/create";
    public static final String VNPAY_CREATE_PATH = "/vnpay/create";
    public static final String VNPAY_CALLBACK_PATH = "/vnpay/callback";
    public static final String USER_PROFILE_PATH = "/profile";
    public static final String BOOKINGS_USER_PATH = "/my-bookings";
    public static final String PAYMENT_DETAIL_PATH = "/booking/{bookingId}";







}
