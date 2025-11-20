# Stage 1: Build stage - Sử dụng JDK để build ứng dụng
FROM eclipse-temurin:21-jdk-jammy as builder

# Đặt thư mục làm việc trong container
WORKDIR /app

# Copy file pom.xml và thư mục .mvn để tận dụng cache layer của Docker
# Chỉ khi pom.xml thay đổi thì các dependency mới được tải lại
COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# Tải tất cả dependency
RUN ./mvnw dependency:go-offline

# Copy toàn bộ source code của dự án
COPY src ./src

# Build ứng dụng, bỏ qua tests để build nhanh hơn
RUN ./mvnw package -DskipTests

# Stage 2: Final stage - Sử dụng JRE để chạy ứng dụng, cho image nhẹ hơn
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy file JAR đã được build từ stage builder
COPY --from=builder /app/target/*.jar app.jar

# Thêm một user không phải root để chạy ứng dụng, tăng cường bảo mật
RUN useradd -ms /bin/bash appuser
USER appuser

# Mở cổng 8080 để có thể truy cập ứng dụng từ bên ngoài container
EXPOSE 8080

# Lệnh để khởi chạy ứng dụng khi container bắt đầu
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]