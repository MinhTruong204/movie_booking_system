-- ============================================================
-- CINEMA BOOKING SYSTEM - COMPLETE SCHEMA
-- Version: 3.0 Final
-- Author: MinhTruong204
-- Date: 2025-10-22
-- Description: Complete database schema with all features
-- ============================================================

-- ============================================================
-- 0. DATABASE SETUP
-- ============================================================

DROP DATABASE IF EXISTS movie_booking_system;
CREATE DATABASE movie_booking_system 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE movie_booking_system;

SET NAMES utf8mb4;
SET sql_mode = 'STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION';

-- ============================================================
-- 1. USER MANAGEMENT
-- ============================================================

-- Membership Tiers (Hạng thành viên)
CREATE TABLE membership_tiers (
    tier_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Member, Silver, Gold, Diamond',
    description TEXT,
    points_required INT NOT NULL DEFAULT 0 COMMENT 'Điểm cần để lên hạng',
    discount_percent DECIMAL(5, 2) DEFAULT 0 COMMENT 'Chiết khấu mặc định (%)',
    birthday_discount DECIMAL(5, 2) DEFAULT 0 COMMENT 'Giảm giá sinh nhật (%)',
    free_tickets_per_year INT DEFAULT 0 COMMENT 'Số vé miễn phí/năm',
    priority_booking BOOLEAN DEFAULT FALSE COMMENT 'Được đặt vé ưu tiên',
    color_code VARCHAR(7) COMMENT 'Màu thẻ: #FFD700',
    icon_url VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_points (points_required)
) ENGINE=InnoDB COMMENT='Các hạng thành viên';

-- Users
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('CUSTOMER', 'ADMIN') NOT NULL DEFAULT 'CUSTOMER',
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    birth_date DATE,
    
    -- Membership fields
    membership_tier_id INT DEFAULT 1,
    loyalty_points INT DEFAULT 0,
    total_spent DECIMAL(15, 2) DEFAULT 0,
    member_since DATE,
    
    -- Account status
    is_active BOOLEAN DEFAULT TRUE,
    email_verified BOOLEAN DEFAULT FALSE,
    phone_verified BOOLEAN DEFAULT FALSE,
    
    -- Security
    last_login_at TIMESTAMP NULL,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP NULL,
    
    -- Soft delete
    deleted_at TIMESTAMP NULL,
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (membership_tier_id) REFERENCES membership_tiers(tier_id),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_role (role),
    INDEX idx_active (is_active),
    INDEX idx_membership (membership_tier_id),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Người dùng hệ thống';

-- Email Verification
CREATE TABLE email_verifications (
    verification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(128) NOT NULL UNIQUE,
    token_type ENUM('REGISTRATION','PASSWORD_RESET','EMAIL_CHANGE') NOT NULL DEFAULT 'REGISTRATION',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    used_at TIMESTAMP NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_expires (expires_at),
    INDEX idx_is_used (is_used)
) ENGINE=InnoDB COMMENT='Bảng lưu token xác minh email (BE sẽ sinh/kiểm tra/huỷ token)';

-- Tạo bảng refresh_tokens để lưu refresh token JWT (rotation + revoke)
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    token VARCHAR(512) NOT NULL UNIQUE,
    expiry_date DATETIME NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_token (token),
    INDEX idx_expiry (expiry_date),
    INDEX idx_revoked (revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Login Attempts
CREATE TABLE login_attempts (
    attempt_id INT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent TEXT,
    success BOOLEAN NOT NULL,
    failure_reason VARCHAR(255),
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_email_time (email, attempted_at),
    INDEX idx_ip_time (ip_address, attempted_at),
    INDEX idx_success (success)
) ENGINE=InnoDB COMMENT='Lịch sử đăng nhập';

-- Loyalty Points History
CREATE TABLE loyalty_points_history (
    history_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    booking_id INT,
    points_change INT NOT NULL COMMENT 'Số điểm thay đổi (+/-)',
    points_type ENUM('EARN', 'REDEEM', 'EXPIRE', 'BONUS', 'ADJUSTMENT') NOT NULL,
    description TEXT,
    old_balance INT NOT NULL,
    new_balance INT NOT NULL,
    expires_at DATE COMMENT 'Điểm có hạn sử dụng',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_booking (booking_id),
    INDEX idx_type (points_type),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='Lịch sử tích điểm';

-- Audit Logs
CREATE TABLE audit_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    action VARCHAR(100) NOT NULL,
    table_name VARCHAR(50),
    record_id INT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_user (user_id),
    INDEX idx_table (table_name),
    INDEX idx_action (action),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='Log hoạt động hệ thống';

-- ============================================================
-- 2. MOVIE CATALOG
-- ============================================================

-- Genres
CREATE TABLE genres (
    genre_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB COMMENT='Thể loại phim';

-- Directors
CREATE TABLE directors (
    director_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    nationality VARCHAR(50),
    bio TEXT,
    photo_url VARCHAR(255),
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Đạo diễn';

-- Actors
CREATE TABLE actors (
    actor_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    nationality VARCHAR(50),
    bio TEXT,
    photo_url VARCHAR(255),
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_name (name),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Diễn viên';

-- Movies
CREATE TABLE movies (
    movie_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    duration INT NOT NULL COMMENT 'Thời lượng (phút)',
    age_rating VARCHAR(10) COMMENT 'P, C13, C16, C18',
    language VARCHAR(50),
    subtitle VARCHAR(100),
    producer VARCHAR(100),
    release_date DATE,
    end_date DATE,
    status ENUM('COMING_SOON', 'NOW_SHOWING', 'ENDED') DEFAULT 'NOW_SHOWING',
    poster_url VARCHAR(255),
    trailer_url VARCHAR(255),
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_release_date (release_date),
    INDEX idx_status_date (status, release_date),
    INDEX idx_deleted (deleted_at),
    FULLTEXT INDEX ft_title_desc (title, description)
) ENGINE=InnoDB COMMENT='Phim';

-- Movie Directors (Many-to-Many)
CREATE TABLE movie_directors (
    movie_id INT NOT NULL,
    director_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (movie_id, director_id),
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
    FOREIGN KEY (director_id) REFERENCES directors(director_id) ON DELETE CASCADE,
    INDEX idx_director (director_id)
) ENGINE=InnoDB COMMENT='Phim - Đạo diễn';

-- Movie Actors (Many-to-Many)
CREATE TABLE movie_actors (
    movie_actor_id INT AUTO_INCREMENT PRIMARY KEY,
    movie_id INT NOT NULL,
    actor_id INT NOT NULL,
    role VARCHAR(100) COMMENT 'Vai diễn',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
    FOREIGN KEY (actor_id) REFERENCES actors(actor_id) ON DELETE CASCADE,
    INDEX idx_movie (movie_id),
    INDEX idx_actor (actor_id)
) ENGINE=InnoDB COMMENT='Phim - Diễn viên';

-- Movie Genres (Many-to-Many)
CREATE TABLE movie_genres (
    movie_id INT NOT NULL,
    genre_id INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (movie_id, genre_id),
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
    FOREIGN KEY (genre_id) REFERENCES genres(genre_id) ON DELETE CASCADE,
    INDEX idx_genre (genre_id)
) ENGINE=InnoDB COMMENT='Phim - Thể loại';

-- Movie Reviews
CREATE TABLE movie_reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    movie_id INT NOT NULL,
    user_id INT NOT NULL,
    rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
    rating_video INT CHECK (rating_video >= 1 AND rating_video <= 5) COMMENT 'Đánh giá hình ảnh',
    rating_audio INT CHECK (rating_audio >= 1 AND rating_audio <= 5) COMMENT 'Đánh giá âm thanh',
    rating_subtitle INT CHECK (rating_subtitle >= 1 AND rating_subtitle <= 5) COMMENT 'Đánh giá phụ đề',
    comment TEXT,
    is_verified_booking BOOLEAN DEFAULT FALSE COMMENT 'User đã xem phim',
    is_approved BOOLEAN DEFAULT TRUE,
    helpful_count INT DEFAULT 0,
    not_helpful_count INT DEFAULT 0,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_movie_user (movie_id, user_id),
    INDEX idx_movie (movie_id),
    INDEX idx_user (user_id),
    INDEX idx_rating (rating),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Đánh giá phim';

-- Review Helpfulness
CREATE TABLE review_helpfulness (
    helpfulness_id INT AUTO_INCREMENT PRIMARY KEY,
    review_id INT NOT NULL,
    user_id INT NOT NULL,
    is_helpful BOOLEAN NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (review_id) REFERENCES movie_reviews(review_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_review_user (review_id, user_id)
) ENGINE=InnoDB COMMENT='Vote review hữu ích';

-- ============================================================
-- 3. CINEMA & ROOM MANAGEMENT
-- ============================================================

-- Seat Types
CREATE TABLE seat_types (
    seat_type_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT 'Regular, VIP, Couple',
    description TEXT,
    price_multiplier DECIMAL(4, 2) NOT NULL DEFAULT 1.0 COMMENT 'Hệ số giá',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active)
) ENGINE=InnoDB COMMENT='Loại ghế';

-- Cinemas
CREATE TABLE cinemas (
    cinema_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255),
    city VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_city (city),
    INDEX idx_active (is_active),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Rạp chiếu';

-- Rooms
CREATE TABLE rooms (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    cinema_id INT NOT NULL,
    name VARCHAR(50) NOT NULL,
    total_seats INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(cinema_id) ON DELETE RESTRICT,
    INDEX idx_cinema (cinema_id),
    INDEX idx_active (is_active),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Phòng chiếu';

-- Seats
CREATE TABLE seats (
    seat_id INT AUTO_INCREMENT PRIMARY KEY,
    room_id INT NOT NULL,
    seat_type_id INT NOT NULL,
    seat_row VARCHAR(10) NOT NULL COMMENT 'Hàng: A, B, C...',
    seat_number INT NOT NULL COMMENT 'Số ghế: 1, 2, 3...',
    is_active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT,
    FOREIGN KEY (seat_type_id) REFERENCES seat_types(seat_type_id) ON DELETE RESTRICT,
    UNIQUE KEY uk_room_seat (room_id, seat_row, seat_number),
    INDEX idx_room (room_id),
    INDEX idx_seat_type (seat_type_id),
    INDEX idx_active (is_active)
) ENGINE=InnoDB COMMENT='Ghế ngồi';

-- Cinema Reviews
CREATE TABLE cinema_reviews (
    review_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    cinema_id INT NOT NULL,
    booking_id INT COMMENT 'Review từ booking thật',
    rating_cleanliness INT CHECK (rating_cleanliness >= 1 AND rating_cleanliness <= 5) COMMENT 'Vệ sinh',
    rating_comfort INT CHECK (rating_comfort >= 1 AND rating_comfort <= 5) COMMENT 'Tiện nghi',
    rating_service INT CHECK (rating_service >= 1 AND rating_service <= 5) COMMENT 'Phục vụ',
    rating_facilities INT CHECK (rating_facilities >= 1 AND rating_facilities <= 5) COMMENT 'Cơ sở vật chất',
    comment TEXT,
    is_verified BOOLEAN DEFAULT FALSE,
    is_approved BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(cinema_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_cinema_booking (user_id, cinema_id, booking_id),
    INDEX idx_cinema (cinema_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='Đánh giá rạp';

-- ============================================================
-- 4. SHOWTIME & BOOKING
-- ============================================================

-- Showtimes
CREATE TABLE showtimes (
    showtime_id INT AUTO_INCREMENT PRIMARY KEY,
    movie_id INT NOT NULL,
    room_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    base_price DECIMAL(10, 2) NOT NULL COMMENT 'Giá vé cơ bản',
    is_active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE RESTRICT,
    FOREIGN KEY (room_id) REFERENCES rooms(room_id) ON DELETE RESTRICT,
    INDEX idx_movie (movie_id),
    INDEX idx_room (room_id),
    INDEX idx_start_time (start_time),
    INDEX idx_movie_time (movie_id, start_time),
    INDEX idx_room_time (room_id, start_time),
    INDEX idx_active (is_active),
    INDEX idx_deleted (deleted_at),
    CONSTRAINT chk_showtime_times CHECK (end_time > start_time)
) ENGINE=InnoDB COMMENT='Suất chiếu';

-- Seat Status (per showtime)
CREATE TABLE seat_status (
    seat_status_id INT AUTO_INCREMENT PRIMARY KEY,
    showtime_id INT NOT NULL,
    seat_id INT NOT NULL,
    status ENUM('AVAILABLE', 'BOOKED', 'HELD') NOT NULL DEFAULT 'AVAILABLE',
    held_by_user_id INT,
    held_until DATETIME,
    version INT DEFAULT 0 COMMENT 'Optimistic locking',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE RESTRICT,
    FOREIGN KEY (held_by_user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    UNIQUE KEY uk_showtime_seat (showtime_id, seat_id),
    INDEX idx_showtime_status (showtime_id, status),
    INDEX idx_held_user (held_by_user_id),
    INDEX idx_held_until (held_until)
) ENGINE=InnoDB COMMENT='Trạng thái ghế theo suất chiếu';

-- Bookings
CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    showtime_id INT NOT NULL,
    booking_code VARCHAR(20) NOT NULL UNIQUE COMMENT 'Mã đặt vé',
    status ENUM('PENDING', 'PAID', 'CANCELLED') NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT 'Tổng tiền trước giảm giá',
    final_amount DECIMAL(10, 2) NOT NULL COMMENT 'Tổng tiền sau giảm giá',
    
    -- E-Ticket fields
    qr_code_data VARCHAR(255) UNIQUE COMMENT 'Dữ liệu QR code',
    qr_code_image_url VARCHAR(255) COMMENT 'URL ảnh QR',
    checked_in_at TIMESTAMP NULL COMMENT 'Thời gian check-in',
    checked_in_by INT COMMENT 'Admin user_id check-in',
    checked_in_location VARCHAR(100) COMMENT 'Vị trí check-in',
    
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id) ON DELETE RESTRICT,
    FOREIGN KEY (checked_in_by) REFERENCES users(user_id) ON DELETE SET NULL,
    
    INDEX idx_user (user_id),
    INDEX idx_showtime (showtime_id),
    INDEX idx_user_status (user_id, status),
    INDEX idx_booking_code (booking_code),
    INDEX idx_qr_code (qr_code_data),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Đơn đặt vé';

-- Booking Seats
CREATE TABLE booking_seats (
    booking_seat_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    seat_id INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL COMMENT 'Giá ghế cuối cùng',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE RESTRICT,
    INDEX idx_booking (booking_id),
    INDEX idx_seat (seat_id)
) ENGINE=InnoDB COMMENT='Ghế đã đặt';

-- Check-in Logs
CREATE TABLE checkin_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    checked_in_by INT COMMENT 'Admin user_id hoặc NULL (self check-in)',
    device_info TEXT,
    location VARCHAR(100),
    ip_address VARCHAR(45),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    INDEX idx_booking (booking_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='Lịch sử check-in';

-- Payments
CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    method VARCHAR(50) COMMENT 'vnpay, momo, zalopay, credit_card',
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    transaction_id VARCHAR(100) UNIQUE,
    gateway_response TEXT COMMENT 'Response từ payment gateway',
    transaction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE RESTRICT,
    INDEX idx_booking (booking_id),
    INDEX idx_transaction_id (transaction_id),
    INDEX idx_status (status),
    INDEX idx_transaction_time (transaction_time)
) ENGINE=InnoDB COMMENT='Thanh toán';

-- ============================================================
-- 5. COMBO & PROMOTION
-- ============================================================

-- Combos
CREATE TABLE combos (
    combo_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    price DECIMAL(10, 2) NOT NULL,
    image_url VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Combo đồ ăn/nước';

-- Booking Combos
CREATE TABLE booking_combos (
    booking_combo_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    combo_id INT NOT NULL,
    quantity INT NOT NULL,
    price DECIMAL(10, 2) NOT NULL COMMENT 'Giá tại thời điểm mua',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (combo_id) REFERENCES combos(combo_id) ON DELETE RESTRICT,
    INDEX idx_booking (booking_id),
    INDEX idx_combo (combo_id)
) ENGINE=InnoDB COMMENT='Combo đã mua';

-- Promotions
CREATE TABLE promotions (
    promo_id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE COMMENT 'Mã khuyến mãi',
    description TEXT,
    discount_type ENUM('PERCENT', 'AMOUNT') NOT NULL COMMENT 'Giảm % hay số tiền',
    discount_value DECIMAL(10, 2) NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    min_order_value DECIMAL(10, 2) DEFAULT 0 COMMENT 'Giá trị đơn hàng tối thiểu',
    max_discount DECIMAL(10, 2) COMMENT 'Giảm tối đa (cho % discount)',
    max_usage INT COMMENT 'Số lượt dùng tổng',
    max_usage_per_user INT DEFAULT 1 COMMENT 'Số lượt dùng/user',
    current_usage INT DEFAULT 0,
    applicable_movies JSON COMMENT 'Array of movie_ids, null = all',
    applicable_days SET('Mon','Tue','Wed','Thu','Fri','Sat','Sun') COMMENT 'null = all days',
    is_active BOOLEAN DEFAULT TRUE,
    deleted_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_code_active (code, is_active),
    INDEX idx_active (is_active),
    INDEX idx_dates (start_date, end_date),
    INDEX idx_deleted (deleted_at)
) ENGINE=InnoDB COMMENT='Khuyến mãi';

-- User Promotion Usage
CREATE TABLE user_promotion_usage (
    usage_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    promo_id INT NOT NULL,
    usage_count INT DEFAULT 0,
    last_used_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (promo_id) REFERENCES promotions(promo_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_promo (user_id, promo_id),
    INDEX idx_user (user_id),
    INDEX idx_promo (promo_id)
) ENGINE=InnoDB COMMENT='Lịch sử dùng mã KM';

-- Booking Promotions
CREATE TABLE booking_promotions (
    booking_promo_id INT AUTO_INCREMENT PRIMARY KEY,
    booking_id INT NOT NULL,
    promo_id INT NOT NULL,
    discount_amount DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    FOREIGN KEY (promo_id) REFERENCES promotions(promo_id) ON DELETE RESTRICT,
    INDEX idx_booking (booking_id),
    INDEX idx_promo (promo_id)
) ENGINE=InnoDB COMMENT='KM đã áp dụng';

-- ============================================================
-- 6. GIFT VOUCHER
-- ============================================================

-- Vouchers
CREATE TABLE vouchers (
    voucher_id INT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    voucher_type ENUM('GIFT_CARD', 'DISCOUNT', 'FREE_TICKET') NOT NULL,
    value DECIMAL(10, 2) NOT NULL COMMENT 'Giá trị',
    original_value DECIMAL(10, 2) NOT NULL,
    current_balance DECIMAL(10, 2) NOT NULL COMMENT 'Số dư còn lại',
    purchased_by INT COMMENT 'User mua voucher',
    recipient_email VARCHAR(100) COMMENT 'Email người nhận',
    recipient_name VARCHAR(100),
    message TEXT COMMENT 'Lời nhắn',
    is_redeemed BOOLEAN DEFAULT FALSE,
    redeemed_by INT COMMENT 'User sử dụng',
    redeemed_at TIMESTAMP NULL,
    expires_at DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (purchased_by) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (redeemed_by) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_code (code),
    INDEX idx_redeemed (is_redeemed),
    INDEX idx_purchased (purchased_by)
) ENGINE=InnoDB COMMENT='Gift voucher';

-- Voucher Usage History
CREATE TABLE voucher_usage_history (
    usage_id INT AUTO_INCREMENT PRIMARY KEY,
    voucher_id INT NOT NULL,
    booking_id INT NOT NULL,
    amount_used DECIMAL(10, 2) NOT NULL,
    balance_before DECIMAL(10, 2) NOT NULL,
    balance_after DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (voucher_id) REFERENCES vouchers(voucher_id) ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE,
    INDEX idx_voucher (voucher_id),
    INDEX idx_booking (booking_id)
) ENGINE=InnoDB COMMENT='Lịch sử dùng voucher';

-- ============================================================
-- 7. REFERRAL PROGRAM
-- ============================================================

-- Referral Codes
CREATE TABLE referral_codes (
    referral_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL COMMENT 'Người giới thiệu',
    code VARCHAR(20) NOT NULL UNIQUE,
    total_uses INT DEFAULT 0,
    total_rewards DECIMAL(10, 2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_code (code),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='Mã giới thiệu';

-- Referral Usage
CREATE TABLE referral_usage (
    usage_id INT AUTO_INCREMENT PRIMARY KEY,
    referral_code_id INT NOT NULL,
    referred_user_id INT NOT NULL COMMENT 'Người được giới thiệu',
    booking_id INT COMMENT 'Booking đầu tiên',
    referrer_reward DECIMAL(10, 2) DEFAULT 0 COMMENT 'Thưởng cho người giới thiệu',
    referee_reward DECIMAL(10, 2) DEFAULT 0 COMMENT 'Thưởng cho người đăng ký',
    status ENUM('PENDING', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (referral_code_id) REFERENCES referral_codes(referral_id) ON DELETE CASCADE,
    FOREIGN KEY (referred_user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE SET NULL,
    INDEX idx_referral (referral_code_id),
    INDEX idx_referred_user (referred_user_id)
) ENGINE=InnoDB COMMENT='Lịch sử giới thiệu';

-- ============================================================
-- 8. WATCHLIST & NOTIFICATIONS
-- ============================================================

-- User Watchlist
CREATE TABLE user_watchlist (
    watchlist_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    movie_id INT NOT NULL,
    notify_on_release BOOLEAN DEFAULT TRUE,
    notify_on_new_showtime BOOLEAN DEFAULT TRUE,
    added_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_movie (user_id, movie_id),
    INDEX idx_user (user_id),
    INDEX idx_movie (movie_id)
) ENGINE=InnoDB COMMENT='Danh sách yêu thích';

-- Notification Preferences
CREATE TABLE notification_preferences (
    pref_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    email_notifications BOOLEAN DEFAULT TRUE,
    sms_notifications BOOLEAN DEFAULT FALSE,
    push_notifications BOOLEAN DEFAULT TRUE,
    notify_new_movies BOOLEAN DEFAULT TRUE,
    notify_promotions BOOLEAN DEFAULT TRUE,
    notify_booking_reminders BOOLEAN DEFAULT TRUE COMMENT 'Nhắc trước 2 giờ',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB COMMENT='Cài đặt thông báo';

-- Notifications
CREATE TABLE notifications (
    notification_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    notification_type VARCHAR(50) NOT NULL COMMENT 'new_movie, new_showtime, promotion, reminder',
    title VARCHAR(255) NOT NULL,
    message TEXT,
    related_type VARCHAR(50) COMMENT 'movie, booking, promotion',
    related_id INT,
    is_read BOOLEAN DEFAULT FALSE,
    read_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_user (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB COMMENT='Thông báo';

-- ============================================================
-- 9. ADVANCED FEATURES
-- ============================================================

-- Advance Booking Rules
CREATE TABLE advance_booking_rules (
    rule_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    days_in_advance INT NOT NULL COMMENT 'Đặt trước bao nhiêu ngày',
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    max_seats_per_booking INT DEFAULT 10,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_active (is_active),
    INDEX idx_days (days_in_advance)
) ENGINE=InnoDB COMMENT='Quy tắc đặt vé sớm';

-- User Seat Preferences
CREATE TABLE user_seat_preferences (
    pref_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    preferred_seat_type_id INT,
    preferred_row_start VARCHAR(10) COMMENT 'Hàng ưa thích: D',
    preferred_row_end VARCHAR(10) COMMENT 'Đến hàng: F',
    preferred_position ENUM('LEFT', 'CENTER', 'RIGHT'),
    avoid_first_row BOOLEAN DEFAULT TRUE,
    avoid_last_row BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (preferred_seat_type_id) REFERENCES seat_types(seat_type_id),
    UNIQUE KEY uk_user (user_id)
) ENGINE=InnoDB COMMENT='Sở thích ghế ngồi';

-- Social Shares
CREATE TABLE social_shares (
    share_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    share_type ENUM('FACEBOOK', 'INSTAGRAM', 'TWITTER', 'ZALO') NOT NULL,
    content_type ENUM('MOVIE', 'BOOKING', 'REVIEW') NOT NULL,
    content_id INT NOT NULL COMMENT 'movie_id hoặc booking_id',
    share_url TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_type (share_type)
) ENGINE=InnoDB COMMENT='Chia sẻ mạng xã hội';

-- Group Bookings
CREATE TABLE group_bookings (
    group_booking_id INT AUTO_INCREMENT PRIMARY KEY,
    organizer_user_id INT NOT NULL COMMENT 'Người tổ chức',
    group_name VARCHAR(100),
    showtime_id INT NOT NULL,
    total_seats INT NOT NULL,
    status ENUM('PENDING', 'CONFIRMED', 'CANCELLED') DEFAULT 'PENDING',
    discount_percent DECIMAL(5, 2) DEFAULT 0,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (organizer_user_id) REFERENCES users(user_id) ON DELETE RESTRICT,
    FOREIGN KEY (showtime_id) REFERENCES showtimes(showtime_id) ON DELETE RESTRICT,
    INDEX idx_organizer (organizer_user_id),
    INDEX idx_showtime (showtime_id)
) ENGINE=InnoDB COMMENT='Đặt vé nhóm';

-- Group Booking Members
CREATE TABLE group_booking_members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    group_booking_id INT NOT NULL,
    user_id INT,
    email VARCHAR(100) COMMENT 'Email người được mời',
    seat_id INT,
    paid_status ENUM('PENDING', 'PAID') DEFAULT 'PENDING',
    invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP NULL,
    FOREIGN KEY (group_booking_id) REFERENCES group_bookings(group_booking_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE SET NULL,
    INDEX idx_group (group_booking_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB COMMENT='Thành viên nhóm';

-- ============================================================
-- 10. STATISTICS (Materialized Views)
-- ============================================================

-- Movie Statistics
CREATE TABLE movie_statistics (
    movie_id INT PRIMARY KEY,
    total_bookings INT DEFAULT 0,
    total_revenue DECIMAL(15, 2) DEFAULT 0,
    average_rating DECIMAL(3, 2) DEFAULT 0,
    total_reviews INT DEFAULT 0,
    total_seats_sold INT DEFAULT 0,
    occupancy_rate DECIMAL(5, 2) DEFAULT 0 COMMENT '% lấp đầy',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(movie_id) ON DELETE CASCADE,
    INDEX idx_revenue (total_revenue DESC),
    INDEX idx_rating (average_rating DESC),
    INDEX idx_bookings (total_bookings DESC)
) ENGINE=InnoDB COMMENT='Thống kê phim';

-- Daily Revenue Summary
CREATE TABLE daily_revenue_summary (
    summary_id INT AUTO_INCREMENT PRIMARY KEY,
    cinema_id INT,
    summary_date DATE NOT NULL,
    total_bookings INT DEFAULT 0,
    total_revenue DECIMAL(15, 2) DEFAULT 0,
    total_tickets_sold INT DEFAULT 0,
    total_combos_sold INT DEFAULT 0,
    combo_revenue DECIMAL(15, 2) DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(cinema_id) ON DELETE CASCADE,
    UNIQUE KEY uk_cinema_date (cinema_id, summary_date),
    INDEX idx_date (summary_date),
    INDEX idx_cinema (cinema_id)
) ENGINE=InnoDB COMMENT='Doanh thu theo ngày';

-- ============================================================
-- END OF SCHEMA
-- ============================================================