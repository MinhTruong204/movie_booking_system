package com.viecinema.showtime.entity;

import com.viecinema.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "seat_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatType extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType. IDENTITY)
    @Column(name = "seat_type_id")
    private Integer seatTypeId;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "price_multiplier", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal priceMultiplier = BigDecimal.ONE;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;


    /**
     * Tính giá cuối cùng dựa trên giá cơ bản
     */
    public BigDecimal calculatePrice(BigDecimal basePrice) {
        return basePrice.multiply(priceMultiplier);
    }

    /**
     * Lấy màu hiển thị theo loại ghế
     */
    public String getColorCode() {
        return switch (name. toLowerCase()) {
            case "regular" -> "#4CAF50";  // Xanh lá
            case "vip" -> "#FF9800";      // Cam
            case "couple" -> "#E91E63";   // Hồng
            case "deluxe" -> "#9C27B0";   // Tím
            default -> "#757575";         // Xám
        };
    }

}