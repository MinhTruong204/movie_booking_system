-- ============================================================
-- 11. SAMPLE DATA
-- ============================================================

-- Insert membership tiers
INSERT INTO membership_tiers (name, description, points_required, discount_percent, birthday_discount, free_tickets_per_year, priority_booking, color_code) VALUES
('Member', 'Hạng thành viên cơ bản', 0, 0, 10, 0, FALSE, '#A0A0A0'),
('Silver', 'Hạng bạc - Ưu đãi tốt hơn', 500, 5, 15, 1, FALSE, '#C0C0C0'),
('Gold', 'Hạng vàng - Nhiều đặc quyền', 2000, 10, 20, 2, TRUE, '#FFD700'),
('Diamond', 'Hạng kim cương - VIP', 5000, 15, 25, 4, TRUE, '#B9F2FF');

-- Insert admin user (password: admin123)
INSERT INTO users (full_name, email, phone, password_hash, role, membership_tier_id, is_active, email_verified, member_since) VALUES
('System Admin', 'admin@cinema.com', '0901234567', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'admin', 4, TRUE, TRUE, '2025-01-01'),
('Customer Demo', 'customer@example.com', '0901234568', '$2y$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'customer', 1, TRUE, TRUE, CURDATE());

-- Insert genres
INSERT INTO genres (name, description) VALUES 
('Action', 'Phim hành động'),
('Drama', 'Phim chính kịch'),
('Comedy', 'Phim hài'),
('Horror', 'Phim kinh dị'),
('Sci-Fi', 'Phim khoa học viễn tưởng'),
('Romance', 'Phim lãng mạn'),
('Thriller', 'Phim ly kỳ'),
('Animation', 'Phim hoạt hình');

-- Insert seat types
INSERT INTO seat_types (name, description, price_multiplier, is_active) VALUES 
('Regular', 'Ghế thường', 1.0, TRUE),
('VIP', 'Ghế VIP - Rộng rãi thoải mái', 1.5, TRUE),
('Couple', 'Ghế đôi - Dành cho cặp đôi', 1.8, TRUE),
('Deluxe', 'Ghế Deluxe - Sang trọng', 2.0, TRUE);

-- Insert sample cinema
INSERT INTO cinemas (name, address, city, phone, email, is_active) VALUES
('CGV Vincom Center', '72 Lê Thánh Tôn, Bến Nghé, Quận 1', 'Hồ Chí Minh', '1900-6017', 'vincom@cgv.vn', TRUE),
('Galaxy Cinema Nguyễn Du', '116 Nguyễn Du, Quận 1', 'Hồ Chí Minh', '1900-2224', 'nguyendu@galaxycine.vn', TRUE),
('Lotte Cinema Diamond Plaza', '34 Lê Duẩn, Quận 1', 'Hồ Chí Minh', '1900-5454', 'diamond@lotte.vn', TRUE);

-- Insert sample rooms
INSERT INTO rooms (cinema_id, name, total_seats, is_active) VALUES
(1, 'P01', 80, TRUE),
(1, 'P02', 100, TRUE),
(2, 'Screen 1', 120, TRUE),
(3, 'Hall A', 150, TRUE);

-- Insert sample combos
INSERT INTO combos (name, description, price, is_active) VALUES
('Combo Single', '1 Bắp Ngọt (M) + 1 Nước Ngọt (M)', 69000, TRUE),
('Combo Couple', '1 Bắp Ngọt (L) + 2 Nước Ngọt (M)', 99000, TRUE),
('Combo Family', '2 Bắp Ngọt (L) + 4 Nước Ngọt (M)', 159000, TRUE),
('Combo Party', '3 Bắp Ngọt (L) + 6 Nước Ngọt (L)', 249000, TRUE);

-- Insert sample promotions
INSERT INTO promotions (code, description, discount_type, discount_value, start_date, end_date, min_order_value, max_usage_per_user, is_active) VALUES
('WELCOME20', 'Giảm 20% cho khách hàng mới', 'percent', 20.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 50000, 1, TRUE),
('FRIDAY50', 'Giảm 50K mỗi thứ 6', 'amount', 50000, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 100000, 5, TRUE),
('BIRTHDAY25', 'Giảm 25% sinh nhật', 'percent', 25.00, '2025-01-01 00:00:00', '2025-12-31 23:59:59', 0, 1, TRUE);

-- Insert advance booking rules
INSERT INTO advance_booking_rules (name, description, days_in_advance, discount_percent, is_active) VALUES
('Early Bird 7 Days', 'Đặt trước 7 ngày - Giảm 10%', 7, 10, TRUE),
('Early Bird 14 Days', 'Đặt trước 14 ngày - Giảm 15%', 14, 15, TRUE);