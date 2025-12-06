package com.viecinema.common.constant;

public final class ApiConstant {
//  Root API
    public static final String AUTH_PATH = "/api/auth";
    public static final String MOVIE_PATH = "/api/movies";
    public static final String SHOWTIMES_PATH = "/api/showtimes";
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


}
