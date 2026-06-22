package com.viecinema.admin.dto.request;

import com.viecinema.common.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Criteria cho tìm kiếm, lọc, sắp xếp và phân trang danh sách user.
 * Tất cả fields đều optional - chỉ áp dụng filter khi field != null.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserSearchCriteria {

    /** Tìm kiếm theo fullName, email hoặc phone (LIKE) */
    private String keyword;

    /** Lọc theo role */
    private Role role;

    /** Lọc theo trạng thái active (true/false) */
    private Boolean isActive;

    /** Lọc user đã bị xóa mềm (true = chỉ hiện đã xóa, false = chỉ hiện chưa xóa) */
    private Boolean isDeleted;

    /** Lọc theo trạng thái xác minh email */
    private Boolean emailVerified;

    /** Lọc theo hạng membership */
    private Integer membershipTierId;

    /** Lọc theo ngày tạo (from) */
    private LocalDate createdFrom;

    /** Lọc theo ngày tạo (to) */
    private LocalDate createdTo;

    /** Lọc theo lần đăng nhập cuối (from) */
    private LocalDate lastLoginFrom;

    /** Lọc theo lần đăng nhập cuối (to) */
    private LocalDate lastLoginTo;

    /** Trường sắp xếp (mặc định: createdAt) */
    private String sortBy = "createdAt";

    /** Hướng sắp xếp (mặc định: desc) */
    private String sortDirection = "desc";

    /** Trang hiện tại (0-indexed, mặc định: 0) */
    private int page = 0;

    /** Số lượng mỗi trang (mặc định: 20) */
    private int size = 20;
}
