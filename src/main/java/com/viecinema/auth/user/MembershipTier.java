package com.viecinema.auth.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA requires no-arg constructor; protected is safer
@AllArgsConstructor
@Builder
@Entity
@Table(name = "membership_tiers")
@ToString(exclude = "description")
public class MembershipTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "tier_id", nullable = false)
    private Integer id;

    @NotNull
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Lob
    @Basic(fetch = FetchType.LAZY) // hint to load lazily; provider-dependent
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @ColumnDefault("0")
    @Builder.Default
    @Column(name = "points_required", nullable = false)
    private Integer pointsRequired = 0;

    @NotNull
    @ColumnDefault("0.00")
    @Builder.Default
    @Digits(integer = 3, fraction = 2) // matches precision=5 scale=2 -> integer digits = 3
    @DecimalMin(value = "0.00", inclusive = true)
    @DecimalMax(value = "100.00", inclusive = true) // only if percent semantics 0-100
    @Column(name = "discount_percent", precision = 5, scale = 2, nullable = false)
    private BigDecimal discountPercent = BigDecimal.ZERO.setScale(2);

    @NotNull
    @ColumnDefault("0.00")
    @Builder.Default
    @Digits(integer = 3, fraction = 2)
    @DecimalMin(value = "0.00", inclusive = true)
    @Column(name = "birthday_discount", precision = 5, scale = 2, nullable = false)
    private BigDecimal birthdayDiscount = BigDecimal.ZERO.setScale(2);

    @NotNull
    @ColumnDefault("0")
    @Builder.Default
    @Column(name = "free_tickets_per_year", nullable = false)
    private Integer freeTicketsPerYear = 0;

    @NotNull
    @ColumnDefault("false")
    @Builder.Default
    @Column(name = "priority_booking", nullable = false)
    private Boolean priorityBooking = Boolean.FALSE;

    @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "Must be a hex color like #RRGGBB")
    @Size(max = 7)
    @Column(name = "color_code", length = 7)
    private String colorCode;

    @Size(max = 255)
    @Column(name = "icon_url")
    private String iconUrl;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}