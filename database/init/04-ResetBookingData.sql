DELIMITER //

DROP PROCEDURE IF EXISTS ResetBookingSystemData //

CREATE PROCEDURE ResetBookingSystemData()
BEGIN
    -- 1. Tắt kiểm tra khóa ngoại để tránh lỗi ràng buộc khi xóa
    SET FOREIGN_KEY_CHECKS = 0;

    -- 2. Xóa sạch dữ liệu các bảng giao dịch & chi tiết (Child Tables)
    TRUNCATE TABLE payments;              -- Lịch sử thanh toán
    TRUNCATE TABLE booking_seats;         -- Ghế trong đơn hàng
    TRUNCATE TABLE booking_combos;        -- Combo trong đơn hàng
    -- TRUNCATE TABLE booking_promotions;    -- Khuyến mãi đã áp dụng
    TRUNCATE TABLE user_promotion_usage;  -- Lịch sử user dùng mã
    TRUNCATE TABLE voucher_usage_history; -- Lịch sử dùng voucher
	-- TRUNCATE TABLE checkin_logs;          -- Log check-in vé
    
    -- 3. Xóa dữ liệu tương tác người dùng
    TRUNCATE TABLE movie_reviews;         -- Review phim
    -- TRUNCATE TABLE cinema_reviews;        -- Review rạp
    -- TRUNCATE TABLE review_helpfulness;    -- Like/Dislike review
    -- TRUNCATE TABLE social_shares;         -- Chia sẻ MXH
    -- TRUNCATE TABLE loyalty_points_history;-- Lịch sử điểm thưởng
--     TRUNCATE TABLE referral_usage;        -- Lịch sử giới thiệu
--     TRUNCATE TABLE notifications;         -- Thông báo
--     TRUNCATE TABLE audit_logs;            -- Log hệ thống
--     TRUNCATE TABLE login_attempts;        -- Lịch sử đăng nhập
--     TRUNCATE TABLE daily_revenue_summary; -- Báo cáo doanh thu

    -- 4. Xóa bảng dữ liệu chính (Parent Tables)
    -- Lưu ý: Không xóa Users, Movies, Cinemas, Showtimes, Rooms, Seats
    -- Chỉ xóa dữ liệu sinh ra trong quá trình vận hành (Bookings)
    TRUNCATE TABLE bookings;

    -- 5. Reset các chỉ số thống kê về 0 (Update Logic)
    
    -- Reset User: Điểm thưởng, tổng chi tiêu
    UPDATE users 
    SET loyalty_points = 0, 
        total_spent = 0,
        last_login_at = NULL;

    -- Reset Phim: Doanh thu, rating, lượt đặt
    UPDATE movie_statistics 
    SET total_bookings = 0, 
        total_revenue = 0, 
        average_rating = 0, 
        total_reviews = 0, 
        total_seats_sold = 0, 
        occupancy_rate = 0;

    -- Reset Voucher: Trả lại tiền như lúc mới tạo
    -- UPDATE vouchers 
--     SET current_balance = original_value, 
--         is_redeemed = 0, 
--         redeemed_by = NULL, 
--         redeemed_at = NULL;

    -- Reset Mã giới thiệu
   --  UPDATE referral_codes 
--     SET total_uses = 0, 
--         total_rewards = 0;

UPDATE seat_status
    SET status = 'AVAILABLE',
        held_by_user_id = NULL,
        held_until = NULL,
        version = 0;

    -- 6. Bật lại kiểm tra khóa ngoại
    SET FOREIGN_KEY_CHECKS = 1;

    -- Trả về thông báo thành công
    SELECT 'SUCCESS' AS Status, 'Hệ thống đã được reset về trạng thái ban đầu (giữ lại master data)' AS Message;
END //

DELIMITER ;