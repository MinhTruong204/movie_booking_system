# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-jammy as builder
WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw .
COPY pom.xml .

# Sửa lỗi quyền thực thi cho file mvnw (Rất quan trọng khi deploy từ Windows)
RUN chmod +x mvnw

# Tải dependency (tận dụng cache)
RUN ./mvnw dependency:go-offline

COPY src ./src
RUN ./mvnw package -DskipTests

# Stage 2: Final stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

# Tạo user để bảo mật
RUN useradd -ms /bin/bash appuser
USER appuser

EXPOSE 8080

# QUAN TRỌNG: Thêm tham số Xmx để giới hạn bộ nhớ Java trong khoảng 300-350MB
# Giúp ứng dụng không bị crash trên gói Free của Render
ENTRYPOINT ["java", "-Xmx350m", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]