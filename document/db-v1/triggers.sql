-- ============================================================
-- CINEMA BOOKING SYSTEM - TRIGGERS & EVENTS (FIXED)
-- Version: 3.0 Final Fixed
-- Author: MinhTruong204
-- Date: 2025-10-23
-- Fix: Delimiter issues and interval syntax
-- ============================================================

USE movie_booking_system;

-- ============================================================
-- IMPORTANT: Set delimiter before creating triggers/events
-- ============================================================

DELIMITER //

-- ============================================================
-- 1. TRIGGERS - BOOKING MANAGEMENT
-- ============================================================

DROP TRIGGER IF EXISTS trg_generate_booking_code//

CREATE TRIGGER trg_generate_booking_code
BEFORE INSERT ON bookings
FOR EACH ROW
BEGIN
    IF NEW.booking_code IS NULL OR NEW.booking_code = '' THEN
        SET NEW.booking_code = CONCAT(
            'BK',
            DATE_FORMAT(NOW(), '%Y%m%d'),
            LPAD(FLOOR(RAND() * 10000), 4, '0')
        );
    END IF;
END//

DROP TRIGGER IF EXISTS trg_generate_qr_code_on_payment//

CREATE TRIGGER trg_generate_qr_code_on_payment
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    IF OLD.status != 'paid' AND NEW.status = 'paid' AND NEW.qr_code_data IS NULL THEN
        UPDATE bookings
        SET qr_code_data = CONCAT(
            'CINEMA-',
            NEW.booking_id, '-', 
            UPPER(LEFT(MD5(CONCAT(NEW.booking_code, NEW.user_id, NOW())), 16))
        )
        WHERE booking_id = NEW.booking_id;
    END IF;
END//

DROP TRIGGER IF EXISTS trg_update_seat_status_on_booking_change//

CREATE TRIGGER trg_update_seat_status_on_booking_change
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    IF OLD.status = 'pending' AND NEW.status = 'paid' THEN
        UPDATE seat_status ss
        JOIN booking_seats bs ON ss.seat_id = bs.seat_id
        SET ss.status = 'booked',
            ss.held_by_user_id = NULL,
            ss.held_until = NULL,
            ss.version = ss.version + 1
        WHERE bs.booking_id = NEW.booking_id
        AND ss.showtime_id = NEW.showtime_id;
    END IF;
    
    IF NEW.status = 'cancelled' THEN
        UPDATE seat_status ss
        JOIN booking_seats bs ON ss.seat_id = bs.seat_id
        SET ss.status = 'available',
            ss.held_by_user_id = NULL,
            ss.held_until = NULL,
            ss.version = ss.version + 1
        WHERE bs.booking_id = NEW.booking_id
        AND ss.showtime_id = NEW.showtime_id;
    END IF;
END//

DROP TRIGGER IF EXISTS trg_audit_booking_status_change//

CREATE TRIGGER trg_audit_booking_status_change
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    IF OLD.status != NEW.status THEN
        INSERT INTO audit_logs (
            user_id,
            action,
            table_name,
            record_id,
            old_value,
            new_value
        ) VALUES (
            NEW.user_id,
            'BOOKING_STATUS_CHANGE',
            'bookings',
            NEW.booking_id,
            OLD.status,
            NEW.status
        );
    END IF;
END//

-- ============================================================
-- 2. TRIGGERS - SHOWTIME VALIDATION
-- ============================================================

DROP TRIGGER IF EXISTS trg_validate_showtime_overlap_insert//

CREATE TRIGGER trg_validate_showtime_overlap_insert
BEFORE INSERT ON showtimes
FOR EACH ROW
BEGIN
    DECLARE overlap_count INT;
    DECLARE buffer_time INT DEFAULT 30;
    
    SELECT COUNT(*) INTO overlap_count
    FROM showtimes
    WHERE room_id = NEW.room_id
    AND deleted_at IS NULL
    AND is_active = TRUE
    AND (
        (NEW.start_time BETWEEN DATE_SUB(start_time, INTERVAL buffer_time MINUTE) 
                            AND DATE_ADD(end_time, INTERVAL buffer_time MINUTE))
        OR (NEW.end_time BETWEEN DATE_SUB(start_time, INTERVAL buffer_time MINUTE) 
                              AND DATE_ADD(end_time, INTERVAL buffer_time MINUTE))
        OR (start_time BETWEEN NEW.start_time AND NEW.end_time)
    );
    
    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Showtime overlaps with existing showtime';
    END IF;
END//

DROP TRIGGER IF EXISTS trg_validate_showtime_overlap_update//

CREATE TRIGGER trg_validate_showtime_overlap_update
BEFORE UPDATE ON showtimes
FOR EACH ROW
BEGIN
    DECLARE overlap_count INT;
    DECLARE buffer_time INT DEFAULT 30;
    
    SELECT COUNT(*) INTO overlap_count
    FROM showtimes
    WHERE room_id = NEW.room_id
    AND showtime_id != NEW.showtime_id
    AND deleted_at IS NULL
    AND is_active = TRUE
    AND (
        (NEW.start_time BETWEEN DATE_SUB(start_time, INTERVAL buffer_time MINUTE) 
                            AND DATE_ADD(end_time, INTERVAL buffer_time MINUTE))
        OR (NEW.end_time BETWEEN DATE_SUB(start_time, INTERVAL buffer_time MINUTE) 
                              AND DATE_ADD(end_time, INTERVAL buffer_time MINUTE))
        OR (start_time BETWEEN NEW.start_time AND NEW.end_time)
    );
    
    IF overlap_count > 0 THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Showtime overlaps with existing showtime';
    END IF;
END//

-- ============================================================
-- 3. TRIGGERS - PROMOTION MANAGEMENT
-- ============================================================

DROP TRIGGER IF EXISTS trg_validate_promotion_usage//

CREATE TRIGGER trg_validate_promotion_usage
BEFORE INSERT ON booking_promotions
FOR EACH ROW
BEGIN
    DECLARE v_max_usage INT;
    DECLARE v_current_usage INT;
    DECLARE v_max_usage_per_user INT;
    DECLARE v_user_usage INT;
    DECLARE v_user_id INT;
    DECLARE v_is_active BOOLEAN;
    DECLARE v_start_date DATETIME;
    DECLARE v_end_date DATETIME;
    DECLARE v_min_order_value DECIMAL(10, 2);
    DECLARE v_total_amount DECIMAL(10, 2);
    
    SELECT 
        max_usage, 
        current_usage, 
        max_usage_per_user, 
        is_active, 
        start_date, 
        end_date,
        min_order_value
    INTO 
        v_max_usage, 
        v_current_usage, 
        v_max_usage_per_user, 
        v_is_active, 
        v_start_date, 
        v_end_date,
        v_min_order_value
    FROM promotions
    WHERE promo_id = NEW.promo_id;
    
    SELECT user_id, total_amount 
    INTO v_user_id, v_total_amount
    FROM bookings 
    WHERE booking_id = NEW.booking_id;
    
    IF v_is_active = FALSE THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Promotion is not active';
    END IF;
    
    IF NOW() < v_start_date OR NOW() > v_end_date THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Promotion is not valid';
    END IF;
    
    IF v_total_amount < v_min_order_value THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Order value does not meet minimum';
    END IF;
    
    IF v_max_usage IS NOT NULL AND v_current_usage >= v_max_usage THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Promotion usage limit reached';
    END IF;
    
    SELECT IFNULL(usage_count, 0) INTO v_user_usage
    FROM user_promotion_usage
    WHERE user_id = v_user_id AND promo_id = NEW.promo_id;
    
    IF v_user_usage >= v_max_usage_per_user THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'User usage limit reached';
    END IF;
END//

DROP TRIGGER IF EXISTS trg_update_promotion_usage//

CREATE TRIGGER trg_update_promotion_usage
AFTER INSERT ON booking_promotions
FOR EACH ROW
BEGIN
    DECLARE v_user_id INT;
    
    SELECT user_id INTO v_user_id FROM bookings WHERE booking_id = NEW.booking_id;
    
    UPDATE promotions
    SET current_usage = current_usage + 1
    WHERE promo_id = NEW.promo_id;
    
    INSERT INTO user_promotion_usage (user_id, promo_id, usage_count, last_used_at)
    VALUES (v_user_id, NEW.promo_id, 1, NOW())
    ON DUPLICATE KEY UPDATE
        usage_count = usage_count + 1,
        last_used_at = NOW();
END//

-- ============================================================
-- 4. TRIGGERS - LOYALTY POINTS MANAGEMENT
-- ============================================================

DROP TRIGGER IF EXISTS trg_award_loyalty_points//

CREATE TRIGGER trg_award_loyalty_points
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    DECLARE v_points INT;
    DECLARE v_current_points INT;
    
    IF OLD.status != 'paid' AND NEW.status = 'paid' THEN
        SET v_points = FLOOR(NEW.final_amount / 10000);
        
        SELECT loyalty_points INTO v_current_points
        FROM users
        WHERE user_id = NEW.user_id;
        
        UPDATE users
        SET loyalty_points = loyalty_points + v_points,
            total_spent = total_spent + NEW.final_amount
        WHERE user_id = NEW.user_id;
        
        INSERT INTO loyalty_points_history (
            user_id, 
            booking_id, 
            points_change, 
            points_type,
            description,
            old_balance,
            new_balance,
            expires_at
        ) VALUES (
            NEW.user_id,
            NEW.booking_id,
            v_points,
            'earn',
            CONCAT('Earned from booking ', NEW.booking_code),
            v_current_points,
            v_current_points + v_points,
            DATE_ADD(CURDATE(), INTERVAL 1 YEAR)
        );
        
        CALL sp_check_membership_upgrade(NEW.user_id);
    END IF;
END//

-- ============================================================
-- 5. TRIGGERS - MOVIE REVIEWS
-- ============================================================

DROP TRIGGER IF EXISTS trg_update_movie_stats_review_insert//

CREATE TRIGGER trg_update_movie_stats_review_insert
AFTER INSERT ON movie_reviews
FOR EACH ROW
BEGIN
    INSERT INTO movie_statistics (movie_id, total_reviews, average_rating, last_updated)
    VALUES (NEW.movie_id, 1, NEW.rating, NOW())
    ON DUPLICATE KEY UPDATE
        total_reviews = total_reviews + 1,
        average_rating = (
            SELECT AVG(rating) 
            FROM movie_reviews 
            WHERE movie_id = NEW.movie_id 
            AND deleted_at IS NULL
        ),
        last_updated = NOW();
END//

DROP TRIGGER IF EXISTS trg_update_movie_stats_review_update//

CREATE TRIGGER trg_update_movie_stats_review_update
AFTER UPDATE ON movie_reviews
FOR EACH ROW
BEGIN
    IF OLD.rating != NEW.rating OR (OLD.deleted_at IS NULL AND NEW.deleted_at IS NOT NULL) THEN
        UPDATE movie_statistics
        SET average_rating = (
                SELECT IFNULL(AVG(rating), 0)
                FROM movie_reviews 
                WHERE movie_id = NEW.movie_id 
                AND deleted_at IS NULL
            ),
            total_reviews = (
                SELECT COUNT(*)
                FROM movie_reviews 
                WHERE movie_id = NEW.movie_id 
                AND deleted_at IS NULL
            ),
            last_updated = NOW()
        WHERE movie_id = NEW.movie_id;
    END IF;
END//

DROP TRIGGER IF EXISTS trg_update_review_helpfulness//

CREATE TRIGGER trg_update_review_helpfulness
AFTER INSERT ON review_helpfulness
FOR EACH ROW
BEGIN
    IF NEW.is_helpful = TRUE THEN
        UPDATE movie_reviews
        SET helpful_count = helpful_count + 1
        WHERE review_id = NEW.review_id;
    ELSE
        UPDATE movie_reviews
        SET not_helpful_count = not_helpful_count + 1
        WHERE review_id = NEW.review_id;
    END IF;
END//

-- ============================================================
-- 6. TRIGGERS - REFERRAL PROGRAM
-- ============================================================

DROP TRIGGER IF EXISTS trg_generate_referral_code//

CREATE TRIGGER trg_generate_referral_code
AFTER INSERT ON users
FOR EACH ROW
BEGIN
    IF NEW.role = 'customer' THEN
        INSERT INTO referral_codes (user_id, code, is_active)
        VALUES (
            NEW.user_id,
            CONCAT(UPPER(LEFT(NEW.full_name, 3)), NEW.user_id),
            TRUE
        );
    END IF;
END//

DROP TRIGGER IF EXISTS trg_update_referral_stats//

CREATE TRIGGER trg_update_referral_stats
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    DECLARE v_referral_code_id INT;
    DECLARE v_referrer_reward DECIMAL(10, 2) DEFAULT 50000;
    DECLARE v_referee_reward DECIMAL(10, 2) DEFAULT 50000;
    
    IF OLD.status != 'paid' AND NEW.status = 'paid' THEN
        SELECT ru.referral_code_id INTO v_referral_code_id
        FROM referral_usage ru
        WHERE ru.referred_user_id = NEW.user_id
        AND ru.status = 'pending'
        LIMIT 1;
        
        IF v_referral_code_id IS NOT NULL THEN
            UPDATE referral_usage
            SET booking_id = NEW.booking_id,
                referrer_reward = v_referrer_reward,
                referee_reward = v_referee_reward,
                status = 'completed'
            WHERE referral_code_id = v_referral_code_id
            AND referred_user_id = NEW.user_id;
            
            UPDATE referral_codes
            SET total_uses = total_uses + 1,
                total_rewards = total_rewards + v_referrer_reward
            WHERE referral_id = v_referral_code_id;
        END IF;
    END IF;
END//

-- ============================================================
-- 7. TRIGGERS - VOUCHER MANAGEMENT
-- ============================================================

DROP TRIGGER IF EXISTS trg_update_voucher_balance//

CREATE TRIGGER trg_update_voucher_balance
AFTER INSERT ON voucher_usage_history
FOR EACH ROW
BEGIN
    UPDATE vouchers
    SET current_balance = NEW.balance_after,
        is_redeemed = (NEW.balance_after = 0)
    WHERE voucher_id = NEW.voucher_id;
END//

-- ============================================================
-- 8. TRIGGERS - MOVIE STATISTICS
-- ============================================================

DROP TRIGGER IF EXISTS trg_update_movie_stats_on_booking//

CREATE TRIGGER trg_update_movie_stats_on_booking
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    DECLARE v_movie_id INT;
    DECLARE v_seats_count INT;
    
    IF OLD.status != 'paid' AND NEW.status = 'paid' THEN
        SELECT s.movie_id INTO v_movie_id
        FROM showtimes s
        WHERE s.showtime_id = NEW.showtime_id;
        
        SELECT COUNT(*) INTO v_seats_count
        FROM booking_seats
        WHERE booking_id = NEW.booking_id;
        
        INSERT INTO movie_statistics (
            movie_id, 
            total_bookings, 
            total_revenue, 
            total_seats_sold,
            last_updated
        )
        VALUES (v_movie_id, 1, NEW.final_amount, v_seats_count, NOW())
        ON DUPLICATE KEY UPDATE
            total_bookings = total_bookings + 1,
            total_revenue = total_revenue + NEW.final_amount,
            total_seats_sold = total_seats_sold + v_seats_count,
            last_updated = NOW();
    END IF;
END//

-- ============================================================
-- 9. TRIGGERS - CHECK-IN LOGGING
-- ============================================================

DROP TRIGGER IF EXISTS trg_log_checkin//

CREATE TRIGGER trg_log_checkin
AFTER UPDATE ON bookings
FOR EACH ROW
BEGIN
    IF OLD.checked_in_at IS NULL AND NEW.checked_in_at IS NOT NULL THEN
        INSERT INTO checkin_logs (
            booking_id,
            checked_in_by,
            location
        ) VALUES (
            NEW.booking_id,
            NEW.checked_in_by,
            NEW.checked_in_location
        );
    END IF;
END//

-- ============================================================
-- STORED PROCEDURES
-- ============================================================

DROP PROCEDURE IF EXISTS sp_check_membership_upgrade//

CREATE PROCEDURE sp_check_membership_upgrade(IN p_user_id INT)
BEGIN
    DECLARE v_current_points INT;
    DECLARE v_current_tier_id INT;
    DECLARE v_new_tier_id INT;
    DECLARE v_tier_name VARCHAR(50);
    
    SELECT loyalty_points, membership_tier_id 
    INTO v_current_points, v_current_tier_id
    FROM users
    WHERE user_id = p_user_id;
    
    SELECT tier_id, name INTO v_new_tier_id, v_tier_name
    FROM membership_tiers
    WHERE points_required <= v_current_points
    ORDER BY points_required DESC
    LIMIT 1;
    
    IF v_new_tier_id > v_current_tier_id THEN
        UPDATE users
        SET membership_tier_id = v_new_tier_id
        WHERE user_id = p_user_id;
        
        INSERT INTO audit_logs (user_id, action, table_name, record_id, new_value)
        VALUES (
            p_user_id,
            'MEMBERSHIP_UPGRADE',
            'users',
            p_user_id,
            CONCAT('Upgraded to ', v_tier_name)
        );
        
        INSERT INTO notifications (
            user_id,
            notification_type,
            title,
            message,
            related_type,
            related_id
        ) VALUES (
            p_user_id,
            'membership_upgrade',
            'Congratulations! Membership Upgraded',
            CONCAT('You have been upgraded to ', v_tier_name, ' tier!'),
            'membership',
            v_new_tier_id
        );
    END IF;
END//

DROP PROCEDURE IF EXISTS sp_calculate_booking_price//

CREATE PROCEDURE sp_calculate_booking_price(
    IN p_booking_id INT,
    OUT p_total_amount DECIMAL(10, 2),
    OUT p_final_amount DECIMAL(10, 2)
)
BEGIN
    DECLARE v_seats_total DECIMAL(10, 2);
    DECLARE v_combos_total DECIMAL(10, 2);
    DECLARE v_promotion_discount DECIMAL(10, 2);
    DECLARE v_membership_discount DECIMAL(10, 2);
    DECLARE v_user_id INT;
    DECLARE v_tier_discount DECIMAL(5, 2);
    
    SELECT b.user_id, mt.discount_percent
    INTO v_user_id, v_tier_discount
    FROM bookings b
    JOIN users u ON b.user_id = u.user_id
    JOIN membership_tiers mt ON u.membership_tier_id = mt.tier_id
    WHERE b.booking_id = p_booking_id;
    
    SELECT IFNULL(SUM(price), 0) INTO v_seats_total
    FROM booking_seats
    WHERE booking_id = p_booking_id;
    
    SELECT IFNULL(SUM(quantity * price), 0) INTO v_combos_total
    FROM booking_combos
    WHERE booking_id = p_booking_id;
    
    SELECT IFNULL(SUM(discount_amount), 0) INTO v_promotion_discount
    FROM booking_promotions
    WHERE booking_id = p_booking_id;
    
    SET p_total_amount = v_seats_total + v_combos_total;
    SET v_membership_discount = p_total_amount * (v_tier_discount / 100);
    SET p_final_amount = p_total_amount - v_promotion_discount - v_membership_discount;
    
    IF p_final_amount < 0 THEN
        SET p_final_amount = 0;
    END IF;
END//

DROP PROCEDURE IF EXISTS sp_hold_seats//

CREATE PROCEDURE sp_hold_seats(
    IN p_showtime_id INT,
    IN p_user_id INT,
    IN p_seat_ids TEXT,
    IN p_hold_minutes INT
)
BEGIN
    DECLARE v_seat_id INT;
    DECLARE v_pos INT DEFAULT 1;
    DECLARE v_next_pos INT;
    DECLARE v_seats_list TEXT;
    DECLARE v_held_until DATETIME;
    
    SET v_seats_list = CONCAT(p_seat_ids, ',');
    SET v_held_until = DATE_ADD(NOW(), INTERVAL p_hold_minutes MINUTE);
    
    WHILE v_pos > 0 DO
        SET v_next_pos = LOCATE(',', v_seats_list, v_pos);
        
        IF v_next_pos > 0 THEN
            SET v_seat_id = CAST(SUBSTRING(v_seats_list, v_pos, v_next_pos - v_pos) AS UNSIGNED);
            
            UPDATE seat_status
            SET status = 'held',
                held_by_user_id = p_user_id,
                held_until = v_held_until,
                version = version + 1
            WHERE showtime_id = p_showtime_id
            AND seat_id = v_seat_id
            AND status = 'available';
            
            SET v_pos = v_next_pos + 1;
        ELSE
            SET v_pos = 0;
        END IF;
    END WHILE;
END//

DROP PROCEDURE IF EXISTS sp_apply_voucher//

CREATE PROCEDURE sp_apply_voucher(
    IN p_voucher_code VARCHAR(50),
    IN p_booking_id INT,
    IN p_amount_to_use DECIMAL(10, 2),
    OUT p_success BOOLEAN,
    OUT p_message VARCHAR(255)
)
BEGIN
    DECLARE v_voucher_id INT;
    DECLARE v_current_balance DECIMAL(10, 2);
    DECLARE v_expires_at DATE;
    DECLARE v_is_redeemed BOOLEAN;
    
    SELECT voucher_id, current_balance, expires_at, is_redeemed
    INTO v_voucher_id, v_current_balance, v_expires_at, v_is_redeemed
    FROM vouchers
    WHERE code = p_voucher_code;
    
    IF v_voucher_id IS NULL THEN
        SET p_success = FALSE;
        SET p_message = 'Voucher not found';
    ELSEIF v_is_redeemed = TRUE THEN
        SET p_success = FALSE;
        SET p_message = 'Voucher has been fully redeemed';
    ELSEIF v_expires_at < CURDATE() THEN
        SET p_success = FALSE;
        SET p_message = 'Voucher has expired';
    ELSEIF p_amount_to_use > v_current_balance THEN
        SET p_success = FALSE;
        SET p_message = 'Insufficient voucher balance';
    ELSE
        INSERT INTO voucher_usage_history (
            voucher_id,
            booking_id,
            amount_used,
            balance_before,
            balance_after
        ) VALUES (
            v_voucher_id,
            p_booking_id,
            p_amount_to_use,
            v_current_balance,
            v_current_balance - p_amount_to_use
        );
        
        SET p_success = TRUE;
        SET p_message = 'Voucher applied successfully';
    END IF;
END//

-- ============================================================
-- SCHEDULED EVENTS
-- ============================================================

DROP EVENT IF EXISTS evt_cleanup_expired_held_seats//

CREATE EVENT evt_cleanup_expired_held_seats
ON SCHEDULE EVERY 1 MINUTE
DO
BEGIN
    UPDATE seat_status
    SET status = 'available',
        held_by_user_id = NULL,
        held_until = NULL,
        version = version + 1
    WHERE status = 'held'
    AND held_until < NOW();
END//

DROP EVENT IF EXISTS evt_cancel_unpaid_bookings//

CREATE EVENT evt_cancel_unpaid_bookings
ON SCHEDULE EVERY 5 MINUTE
DO
BEGIN
    UPDATE bookings
    SET status = 'cancelled'
    WHERE status = 'pending'
    AND created_at < DATE_SUB(NOW(), INTERVAL 15 MINUTE);
END//

DROP EVENT IF EXISTS evt_update_movie_status//

CREATE EVENT evt_update_movie_status
ON SCHEDULE EVERY 1 HOUR
DO
BEGIN
    UPDATE movies
    SET status = 'now_showing'
    WHERE status = 'coming_soon'
    AND release_date <= CURDATE()
    AND deleted_at IS NULL;
    
    UPDATE movies
    SET status = 'ended'
    WHERE status = 'now_showing'
    AND end_date < CURDATE()
    AND deleted_at IS NULL;
END//

DROP EVENT IF EXISTS evt_expire_loyalty_points//

CREATE EVENT evt_expire_loyalty_points
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 2 HOUR
DO
BEGIN
    DECLARE done INT DEFAULT FALSE;
    DECLARE v_history_id INT;
    DECLARE v_user_id INT;
    DECLARE v_points_change INT;
    DECLARE v_old_balance INT;
    DECLARE v_new_balance INT;
    
    DECLARE cur CURSOR FOR
        SELECT history_id, user_id, points_change, new_balance
        FROM loyalty_points_history
        WHERE points_type = 'earn'
        AND expires_at = CURDATE();
    
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = TRUE;
    
    OPEN cur;
    
    read_loop: LOOP
        FETCH cur INTO v_history_id, v_user_id, v_points_change, v_old_balance;
        IF done THEN
            LEAVE read_loop;
        END IF;
        
        UPDATE users
        SET loyalty_points = loyalty_points - v_points_change
        WHERE user_id = v_user_id;
        
        SELECT loyalty_points INTO v_new_balance
        FROM users
        WHERE user_id = v_user_id;
        
        INSERT INTO loyalty_points_history (
            user_id,
            points_change,
            points_type,
            description,
            old_balance,
            new_balance
        ) VALUES (
            v_user_id,
            -v_points_change,
            'expire',
            'Points expired after 1 year',
            v_old_balance,
            v_new_balance
        );
    END LOOP;
    
    CLOSE cur;
END//

DROP EVENT IF EXISTS evt_generate_daily_revenue_summary//

CREATE EVENT evt_generate_daily_revenue_summary
ON SCHEDULE EVERY 1 DAY
STARTS CURRENT_DATE + INTERVAL 1 DAY + INTERVAL 1 HOUR
DO
BEGIN
    DECLARE v_yesterday DATE;
    SET v_yesterday = DATE_SUB(CURDATE(), INTERVAL 1 DAY);
    
    INSERT INTO daily_revenue_summary (
        cinema_id,
        summary_date,
        total_bookings,
        total_revenue,
        total_tickets_sold,
        total_combos_sold,
        combo_revenue
    )
    SELECT
        c.cinema_id,
        v_yesterday,
        COUNT(DISTINCT b.booking_id) as total_bookings,
        IFNULL(SUM(b.final_amount), 0) as total_revenue,
        IFNULL(SUM(seat_count.cnt), 0) as total_tickets_sold,
        IFNULL(SUM(bc.quantity), 0) as total_combos_sold,
        IFNULL(SUM(bc.quantity * bc.price), 0) as combo_revenue
    FROM cinemas c
    LEFT JOIN rooms r ON c.cinema_id = r.cinema_id
    LEFT JOIN showtimes s ON r.room_id = s.room_id
    LEFT JOIN bookings b ON s.showtime_id = b.showtime_id
        AND b.status = 'paid'
        AND DATE(b.created_at) = v_yesterday
    LEFT JOIN (
        SELECT booking_id, COUNT(*) as cnt
        FROM booking_seats
        GROUP BY booking_id
    ) seat_count ON b.booking_id = seat_count.booking_id
    LEFT JOIN booking_combos bc ON b.booking_id = bc.booking_id
    WHERE c.deleted_at IS NULL
    GROUP BY c.cinema_id
    ON DUPLICATE KEY UPDATE
        total_bookings = VALUES(total_bookings),
        total_revenue = VALUES(total_revenue),
        total_tickets_sold = VALUES(total_tickets_sold),
        total_combos_sold = VALUES(total_combos_sold),
        combo_revenue = VALUES(combo_revenue),
        updated_at = NOW();
END//

DROP EVENT IF EXISTS evt_send_booking_reminders//

CREATE EVENT evt_send_booking_reminders
ON SCHEDULE EVERY 30 MINUTE
DO
BEGIN
    INSERT INTO notifications (
        user_id,
        notification_type,
        title,
        message,
        related_type,
        related_id
    )
    SELECT
        b.user_id,
        'booking_reminder',
        'Movie Reminder',
        CONCAT('Your movie "', m.title, '" starts in 2 hours at ', 
               DATE_FORMAT(s.start_time, '%H:%i')),
        'booking',
        b.booking_id
    FROM bookings b
    JOIN showtimes s ON b.showtime_id = s.showtime_id
    JOIN movies m ON s.movie_id = m.movie_id
    JOIN users u ON b.user_id = u.user_id
    JOIN notification_preferences np ON u.user_id = np.user_id
    WHERE b.status = 'paid'
    AND s.start_time BETWEEN DATE_ADD(NOW(), INTERVAL 2 HOUR) 
                         AND DATE_ADD(NOW(), INTERVAL 150 MINUTE)
    AND np.notify_booking_reminders = TRUE
    AND NOT EXISTS (
        SELECT 1 FROM notifications n
        WHERE n.user_id = b.user_id
        AND n.related_type = 'booking'
        AND n.related_id = b.booking_id
        AND n.notification_type = 'booking_reminder'
    );
END//

DROP EVENT IF EXISTS evt_notify_watchlist_new_showtimes//

CREATE EVENT evt_notify_watchlist_new_showtimes
ON SCHEDULE EVERY 1 HOUR
DO
BEGIN
    INSERT INTO notifications (
        user_id,
        notification_type,
        title,
        message,
        related_type,
        related_id
    )
    SELECT DISTINCT
        w.user_id,
        'new_showtime',
        'New Showtime Available',
        CONCAT('New showtimes added for "', m.title, '"'),
        'movie',
        m.movie_id
    FROM user_watchlist w
    JOIN movies m ON w.movie_id = m.movie_id
    JOIN showtimes s ON m.movie_id = s.movie_id
    WHERE w.notify_on_new_showtime = TRUE
    AND s.created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
    AND NOT EXISTS (
        SELECT 1 FROM notifications n
        WHERE n.user_id = w.user_id
        AND n.related_type = 'movie'
        AND n.related_id = m.movie_id
        AND n.notification_type = 'new_showtime'
        AND n.created_at >= DATE_SUB(NOW(), INTERVAL 1 HOUR)
    );
END//

-- ============================================================
-- UTILITY FUNCTIONS
-- ============================================================

DROP FUNCTION IF EXISTS fn_calculate_advance_discount//

CREATE FUNCTION fn_calculate_advance_discount(p_showtime_date DATETIME)
RETURNS DECIMAL(5, 2)
DETERMINISTIC
BEGIN
    DECLARE v_days_diff INT;
    DECLARE v_discount DECIMAL(5, 2);
    
    SET v_days_diff = DATEDIFF(p_showtime_date, NOW());
    
    SELECT IFNULL(MAX(discount_percent), 0) INTO v_discount
    FROM advance_booking_rules
    WHERE days_in_advance <= v_days_diff
    AND is_active = TRUE
    ORDER BY discount_percent DESC
    LIMIT 1;
    
    RETURN v_discount;
END//

DROP FUNCTION IF EXISTS fn_is_seat_available//

CREATE FUNCTION fn_is_seat_available(
    p_showtime_id INT,
    p_seat_id INT
)
RETURNS BOOLEAN
DETERMINISTIC
BEGIN
    DECLARE v_status VARCHAR(20);
    
    SELECT status INTO v_status
    FROM seat_status
    WHERE showtime_id = p_showtime_id
    AND seat_id = p_seat_id;
    
    RETURN (v_status = 'available');
END//

DROP FUNCTION IF EXISTS fn_get_user_free_tickets//

CREATE FUNCTION fn_get_user_free_tickets(p_user_id INT)
RETURNS INT
DETERMINISTIC
BEGIN
    DECLARE v_free_tickets INT;
    DECLARE v_used_tickets INT;
    
    SELECT mt.free_tickets_per_year INTO v_free_tickets
    FROM users u
    JOIN membership_tiers mt ON u.membership_tier_id = mt.tier_id
    WHERE u.user_id = p_user_id;
    
    SELECT COUNT(*) INTO v_used_tickets
    FROM bookings b
    WHERE b.user_id = p_user_id
    AND b.final_amount = 0
    AND YEAR(b.created_at) = YEAR(NOW());
    
    RETURN GREATEST(0, v_free_tickets - v_used_tickets);
END//

DELIMITER ;

-- ============================================================
-- ENABLE EVENT SCHEDULER
-- ============================================================

SET GLOBAL event_scheduler = ON;

-- ============================================================
-- VERIFICATION QUERIES
-- ============================================================

SELECT 'Triggers created:' as status, COUNT(*) as count
FROM information_schema.TRIGGERS
WHERE TRIGGER_SCHEMA = 'movie_booking_system';

SELECT 'Events created:' as status, COUNT(*) as count
FROM information_schema.EVENTS
WHERE EVENT_SCHEMA = 'movie_booking_system';

SELECT 'Procedures created:' as status, COUNT(*) as count
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'movie_booking_system'
AND ROUTINE_TYPE = 'PROCEDURE';

SELECT 'Functions created:' as status, COUNT(*) as count
FROM information_schema.ROUTINES
WHERE ROUTINE_SCHEMA = 'movie_booking_system'
AND ROUTINE_TYPE = 'FUNCTION';

-- ============================================================
-- END OF TRIGGERS & EVENTS (FIXED)
-- ============================================================