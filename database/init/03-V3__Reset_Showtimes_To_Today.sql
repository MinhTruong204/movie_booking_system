DELIMITER //

DROP PROCEDURE IF EXISTS refresh_database_date//

CREATE PROCEDURE refresh_database_date()
BEGIN
    DECLARE day_offset INT DEFAULT 0;
    DECLARE current_date_val DATE;
    DECLARE base_date DATE;
    
    -- 1. DỌN DẸP DỮ LIỆU CŨ
    -- Tắt kiểm tra khóa ngoại tạm thời để xóa cho nhanh
    SET FOREIGN_KEY_CHECKS = 0;
    TRUNCATE TABLE booking_seats;
    TRUNCATE TABLE booking_combos;
    TRUNCATE TABLE payments;
    TRUNCATE TABLE bookings;
    TRUNCATE TABLE seat_status;
    TRUNCATE TABLE showtimes;
    SET FOREIGN_KEY_CHECKS = 1;

    -- 2. TẠO SHOWTIMES MỚI (Logic gốc của bạn)
    SET base_date = CURDATE(); -- Lấy ngày hiện tại lúc chạy lệnh
    
    WHILE day_offset < 7 DO
        SET current_date_val = DATE_ADD(base_date, INTERVAL day_offset DAY);
        
        -- PHIM 1: Avengers: Endgame
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (1, 4, CONCAT(current_date_val, ' 14:00:00'), CONCAT(current_date_val, ' 17:01:00'), 95000, TRUE);
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (1, 5, CONCAT(current_date_val, ' 19:30:00'), CONCAT(current_date_val, ' 22:31:00'), 95000, TRUE);
        
        -- PHIM 2: Kỵ Sĩ Bóng Đêm
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (2, 3, CONCAT(current_date_val, ' 10:00:00'), CONCAT(current_date_val, ' 12:32:00'), 95000, TRUE);
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (2, 1, CONCAT(current_date_val, ' 20:00:00'), CONCAT(current_date_val, ' 22:32:00'), 95000, TRUE);
        
        -- PHIM 3: Nhà Tù Shawshank
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (3, 1, CONCAT(current_date_val, ' 13:00:00'), CONCAT(current_date_val, ' 15:22:00'), 85000, TRUE);
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (3, 2, CONCAT(current_date_val, ' 17:00:00'), CONCAT(current_date_val, ' 19:22:00'), 85000, TRUE);
        
        -- PHIM 4: Vua Sư Tử
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (4, 2, CONCAT(current_date_val, ' 09:00:00'), CONCAT(current_date_val, ' 10:28:00'), 75000, TRUE);
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (4, 3, CONCAT(current_date_val, ' 15:00:00'), CONCAT(current_date_val, ' 16:28:00'), 75000, TRUE);
        
        -- PHIM 5: Coco
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (5, 4, CONCAT(current_date_val, ' 10:30:00'), CONCAT(current_date_val, ' 12:15:00'), 75000, TRUE);
        INSERT INTO showtimes (movie_id, room_id, start_time, end_time, base_price, is_active)
        VALUES (5, 5, CONCAT(current_date_val, ' 16:30:00'), CONCAT(current_date_val, ' 18:15:00'), 75000, TRUE);
        
        SET day_offset = day_offset + 1;
    END WHILE;

    -- 3. TẠO LẠI SEAT STATUS (Quan trọng: Nếu không có bước này, có lịch nhưng không đặt được ghế)
    INSERT INTO seat_status (showtime_id, seat_id, status, version)
    SELECT sh.showtime_id, s.seat_id, 'available', 0
    FROM showtimes sh
    CROSS JOIN seats s
    WHERE s.room_id = sh.room_id
      AND sh.is_active = TRUE
      AND s.is_active = TRUE;
      
    -- Thông báo hoàn tất (Option)
    SELECT 'Data reset successfully to current date!' as message;
END//

DELIMITER ;