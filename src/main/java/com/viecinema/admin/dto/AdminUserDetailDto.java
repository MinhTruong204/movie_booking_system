package com.viecinema.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO chi tiết user cho Admin detail view.
 * Chứa đầy đủ thông tin bao gồm membership, security, audit fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDetailDto {

    // Thông tin cơ bản
    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String gender;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    // Trạng thái tài khoản
    private Boolean isActive;
    private Boolean emailVerified;
    private Boolean phoneVerified;

    // Membership
    private String membershipTierName;
    private Integer membershipTierId;
    private Integer loyaltyPoints;
    private BigDecimal totalSpent;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate memberSince;

    // Security
    private Integer failedLoginAttempts;
    private Instant lockedUntil;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    // Audit
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;
}
