# VieCinema - Movie Booking System

<div align="center">

![Java Version](https://img.shields.io/badge/Java-21-orange?style=flat-square)
![Spring Boot Version](https://img.shields.io/badge/Spring%20Boot-3.3.1-brightgreen?style=flat-square)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)
![Status](https://img.shields.io/badge/Status-Active-success?style=flat-square)

**Nền tảng đặt vé xem phim hiện đại, toàn diện và an toàn**

[Tính Năng](#-tính-năng-cốt-lõi) • [Công Nghệ](#-công-nghệ-sử-dụng) • [Kiến Trúc](#-kiến-trúc-hệ-thống) • [Cài Đặt](#-hướng-dẫn-cài-đặt) • [API Docs](#-api-documentation)

</div>

---

## 📋 Giới Thiệu

**VieCinema** là hệ thống quản lý và đặt vé xem phim toàn diện được xây dựng trên nền tảng **Spring Boot 3** và **Java 21**. Dự án cung cấp giải pháp end-to-end cho luồng nghiệp vụ rạp chiếu phim: từ quản lý danh mục phim, lịch chiếu, sơ đồ ghế động (dynamic seatmap), cho đến quy trình giữ chỗ (seat holding) và tích hợp thanh toán trực tuyến qua VNPay.

---

## ✨ Tính Năng Cốt Lõi

### 🔐 Xác Thực & Bảo Mật (Security)
* **JWT Authentication:** Tích hợp Access Token & Refresh Token (HTTP-only Cookie).
* **Role-based Access Control (RBAC):** Phân quyền nghiêm ngặt giữa `CUSTOMER` và `ADMIN`.
* **Security Mechanics:** Tự động khóa tài khoản khi nhập sai mật khẩu nhiều lần, theo dõi lịch sử đăng nhập (IP, User Agent).

### 🎟️ Nghiệp Vụ Đặt Vé (Core Booking Flow)
* **Quản lý Ghế Thông Minh:** Trạng thái ghế realtime (Available, Booked, Held). Hỗ trợ cơ chế **Seat Holding** (giữ ghế tạm thời trong 10 phút) và tự động giải phóng (release) nếu không hoàn tất thanh toán.
* **Pricing & Phụ Trợ:** Tính giá động theo loại ghế (Standard, Premium, VIP) và tích hợp bán Combo (Popcorn, Drinks).
* **Thanh Toán VNPay:** Tích hợp cổng thanh toán VNPay, xử lý callback an toàn và ghi nhận giao dịch.

### 🏢 Quản Trị Hệ Thống (Admin & Management)
* **Quản lý Rạp & Phòng Chiếu:** Thiết lập cấu hình phòng chiếu, sơ đồ ghế linh hoạt.
* **Quản lý Phim & Suất Chiếu:** Phân loại phim (Đang chiếu, Sắp chiếu), lọc suất chiếu thông minh theo ngày/rạp, tích hợp Caching để tối ưu tốc độ truy xuất.
* **Quản lý Người Dùng:** Thống kê lịch sử đặt vé, hệ thống Membership Tiers và Loyalty Points.

### ⚙️ Tính Năng Nâng Cao
* **Soft Delete & Audit Trail:** Theo dõi dấu vết thay đổi dữ liệu (Created/Updated/Deleted).
* **Scheduled Tasks:** Hệ thống tự động dọn dẹp dữ liệu rác, hủy đơn quá hạn bằng Spring Scheduling.
* **API Documentation:** Tích hợp Swagger/OpenAPI tự động.

---

## 🛠️ Công Nghệ Sử Dụng

| Phân Loại | Công Nghệ / Thư Viện | Mô Tả |
| :--- | :--- | :--- |
| **Backend Core** | Java 21, Spring Boot 3.3.1 | Ngôn ngữ và framework chính |
| **Security** | Spring Security, JJWT (0.12.6) | Xác thực và phân quyền |
| **Database & ORM** | MySQL 8.0, Spring Data JPA | Quản trị và truy xuất CSDL |
| **Performance** | Spring Cache | Caching dữ liệu (Genres, Movies, Showtimes) |
| **Utilities** | MapStruct (1.5.5), Lombok | Mapping DTO và giảm boilerplate code |
| **DevOps** | Docker, Docker Compose, Maven | Đóng gói và triển khai môi trường |
| **Documentation** | SpringDoc OpenAPI 2.6.0 | Tài liệu hóa RESTful API |

---

## 🏗️ Kiến Trúc Hệ Thống

### Tầng Kiến Trúc (Layered Architecture)

```text
┌─────────────────────────────────────────┐
│       REST API / HTTP Requests          │
├─────────────────────────────────────────┤
│       Controller (Request/Response)     │
├─────────────────────────────────────────┤
│       Service (Business Logic)          │
├─────────────────────────────────────────┤
│       Repository (Data Access)          │
├─────────────────────────────────────────┤
│       MySQL Database (Data Layer)       │
└─────────────────────────────────────────┘
* Cross-cutting: Security (JWT), Exception Handling, Caching, Logging (SLF4J)
```

### Luồng Đặt Vé (Booking Flow)

```text
Browse Movies ──> Select Showtime ──> View Seatmap ──> Hold Seats (10 mins)
                                                               │
     [Auto Release if expired] <── [Pending Booking] <── Calculate Price (Seats + Combos)
                                                               │
      Payment Completed ✓ ──> VNPay Processing ────────────────┘

```

---

## 🚀 Hướng Dẫn Cài Đặt

### 1. Yêu Cầu Hệ Thống

* JDK 21+
* Maven 3.8+
* MySQL 8.0
* Docker (Tùy chọn cho môi trường container)

### 2. Cấu Hình Biến Môi Trường

Tạo file `.env` hoặc cấu hình trực tiếp trong `application.properties`/`docker-compose.yml`:

```properties
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/movie_booking_system
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=your_password

JWT_ACCESS_TOKEN_SECRET=your_secret_key_at_least_256_bits
JWT_REFRESH_TOKEN_SECRET=your_refresh_secret_key

VNPAY_TMN_CODE=your_merchant_code
VNPAY_HASH_SECRET=your_hash_secret

```

### 3. Khởi Chạy Ứng Dụng

**Sử dụng Maven (Local):**

```bash
# Clone repository
git clone [https://github.com/MinhTruong204/movie_booking_system.git](https://github.com/MinhTruong204/movie_booking_system.git)
cd movie_booking_system

# Build và chạy ứng dụng
./mvnw clean install
./mvnw spring-boot:run

```

**Sử dụng Docker Compose:**

```bash
docker-compose up -d --build

```

---

## 📚 API Documentation

Ứng dụng cung cấp giao diện Swagger UI tương tác đầy đủ tại:

🔗 **`http://localhost:8080/swagger-ui.html`**

**Danh sách Endpoints Chính:**

| Module | Method | Endpoint | Chức Năng |
| --- | --- | --- | --- |
| **Auth** | `POST` | `/api/auth/login` | Đăng nhập & Nhận Token |
| **Auth** | `POST` | `/api/auth/refresh-token` | Cấp lại Access Token |
| **Movies** | `GET` | `/api/movies/now-showing` | Lấy danh sách phim đang chiếu |
| **Showtimes** | `GET` | `/api/showtimes/{id}/seatmap` | Xem sơ đồ ghế của suất chiếu |
| **Booking** | `POST` | `/api/bookings/hold-seats` | Giữ ghế tạm thời |
| **Booking** | `POST` | `/api/bookings` | Tạo đơn đặt vé |
| **Payment** | `POST` | `/api/payments/vnpay-callback` | Xử lý webhook từ VNPay |

---

---

## 🗺️ Roadmap Phát Triển

* **Phase 1 (Hoàn thành):** Core booking logic, JWT Auth, VNPay, Caching, Swagger.
* **Phase 2 (Sắp tới):** Tích hợp hoàn tiền (Refund), Hệ thống Đánh giá (Review/Rating), Gửi Email/SMS thông báo vé.
* **Phase 3 (Tương lai):** Phát triển Mobile App (Flutter/React Native), Admin Dashboard nâng cao (React/Vue), WebSocket thông báo Real-time.

---

## 🤝 Đóng Góp (Contributing)

Mọi đóng góp nhằm cải thiện hệ thống đều được chào đón! Vui lòng:

1. Fork dự án
2. Tạo Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit thay đổi (`git commit -m 'feat: Add some AmazingFeature'`)
4. Push lên Branch (`git push origin feature/AmazingFeature`)
5. Mở Pull Request

---

## 📄 Giấy Phép & Liên Hệ

* **License:** Phân phối dưới giấy phép [MIT License](https://www.google.com/search?q=LICENSE).
* **Tác giả:** **Minh Truong** - [GitHub Profile](https://github.com/MinhTruong204)
* **Báo cáo lỗi:** Vui lòng tạo [Issue](https://github.com/MinhTruong204/movie_booking_system/issues) trên GitHub.
