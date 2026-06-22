package com.viecinema.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO nhẹ cho danh sách user (Admin list view).
 * Không chứa membership detail để giảm payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserListDto {

    private Integer userId;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String gender;
    private Boolean isActive;
    private Boolean emailVerified;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deletedAt;
}
