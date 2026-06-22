package com.viecinema.admin.service;

import com.viecinema.admin.dto.*;
import com.viecinema.admin.dto.request.*;
import com.viecinema.admin.event.UserAction;
import com.viecinema.admin.event.UserStatusChangedEvent;
import com.viecinema.admin.mapper.AdminUserMapper;
import com.viecinema.admin.specification.UserSpecification;
import com.viecinema.auth.entity.RefreshToken;
import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.RefreshTokenRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.enums.Role;
import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.DuplicateResourceException;
import com.viecinema.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service chính xử lý toàn bộ business logic cho Admin User Management.
 * <p>
 * Design principle: Service chỉ phát event (Observer Pattern),
 * KHÔNG trực tiếp xử lý side-effect. Listener tự lắng nghe và xử lý.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminUserMapper adminUserMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ============ CRUD Operations ============

    /**
     * Lấy danh sách user với search/filter/sort/pagination
     */
    @Transactional(readOnly = true)
    public Page<AdminUserListDto> getUsers(UserSearchCriteria criteria) {
        log.info("Admin fetching users with criteria: keyword={}, role={}, page={}, size={}",
                criteria.getKeyword(), criteria.getRole(), criteria.getPage(), criteria.getSize());

        // Validate pagination
        if (criteria.getSize() < 1 || criteria.getSize() > 100) {
            criteria.setSize(20);
        }
        if (criteria.getPage() < 0) {
            criteria.setPage(0);
        }

        // Build sort
        Sort sort = buildSort(criteria.getSortBy(), criteria.getSortDirection());
        Pageable pageable = PageRequest.of(criteria.getPage(), criteria.getSize(), sort);

        // Build specification from criteria
        Specification<User> spec = UserSpecification.buildFromCriteria(criteria);

        // Query and map
        Page<User> userPage = userRepository.findAll(spec, pageable);
        return userPage.map(adminUserMapper::toListDto);
    }

    /**
     * Lấy chi tiết user (kể cả đã soft delete, cho admin xem)
     */
    @Transactional(readOnly = true)
    public AdminUserDetailDto getUserById(Integer userId) {
        log.info("Admin fetching user detail for ID: {}", userId);
        User user = findUserOrThrow(userId);
        return adminUserMapper.toDetailDto(user);
    }

    // Create User
    @Transactional
    public AdminUserDetailDto createUser(AdminCreateUserRequest request, Integer adminId) {
        log.info("Admin {} creating new user with email: {}", adminId, request.getEmail());

        // Validate unique constraints
        String normalizedEmail = request.getEmail().toLowerCase();
        if (userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail)) {
            throw new DuplicateResourceException("Email");
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String normalizedPhone = normalizePhone(request.getPhone());
            if (userRepository.existsByPhoneAndDeletedAtIsNull(normalizedPhone)) {
                throw new DuplicateResourceException("Phone");
            }
            request.setPhone(normalizedPhone);
        }

        // Map and set additional fields
        User user = adminUserMapper.createRequestToUser(request);
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        // Publish event
        publishEvent(savedUser.getId(), savedUser.getEmail(), UserAction.CREATED, adminId,
                "Admin created new user");

        log.info("User created successfully with ID: {}", savedUser.getId());
        return adminUserMapper.toDetailDto(savedUser);
    }

    /**
     * Cập nhật thông tin user (partial update)
     */
    @Transactional
    public AdminUserDetailDto updateUser(Integer userId, AdminUpdateUserRequest request, Integer adminId) {
        log.info("Admin {} updating user ID: {}", adminId, userId);

        User user = findActiveUserOrThrow(userId);

        // Validate unique constraints nếu email/phone thay đổi
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            String normalizedEmail = request.getEmail().toLowerCase();
            if (userRepository.existsByEmailAndIdNotAndDeletedAtIsNull(normalizedEmail, userId)) {
                throw new DuplicateResourceException("Email");
            }
            user.setEmail(normalizedEmail);
        }

        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            String normalizedPhone = normalizePhone(request.getPhone());
            if (userRepository.existsByPhoneAndIdNotAndDeletedAtIsNull(normalizedPhone, userId)) {
                throw new DuplicateResourceException("Phone");
            }
            user.setPhone(normalizedPhone);
        }

        // Partial update - chỉ set khi field != null
        if (request.getFullName() != null) user.setFullName(request.getFullName());
        if (request.getGender() != null) user.setGender(request.getGender());
        if (request.getBirthDate() != null) user.setBirthDate(request.getBirthDate());
        if (request.getIsActive() != null) user.setIsActive(request.getIsActive());
        if (request.getEmailVerified() != null) user.setEmailVerified(request.getEmailVerified());
        if (request.getPhoneVerified() != null) user.setPhoneVerified(request.getPhoneVerified());

        User updatedUser = userRepository.save(user);

        publishEvent(userId, updatedUser.getEmail(), UserAction.UPDATED, adminId, "Admin updated user info");

        log.info("User {} updated successfully", userId);
        return adminUserMapper.toDetailDto(updatedUser);
    }

    /**
     * Xóa mềm user (Soft Delete)
     */
    @Transactional
    public void softDeleteUser(Integer userId, Integer adminId, String reason) {
        log.info("Admin {} soft deleting user ID: {} - Reason: {}", adminId, userId, reason);

        User user = findActiveUserOrThrow(userId);
        preventSelfAction(userId, adminId, "delete");

        user.setDeletedAt(LocalDateTime.now());
        user.setIsActive(false);
        userRepository.save(user);

        // Event → SessionEventListener sẽ tự revoke sessions
        publishEvent(userId, user.getEmail(), UserAction.SOFT_DELETED, adminId, reason);

        log.info("User {} soft deleted successfully", userId);
    }

    /**
     * Khôi phục user đã bị xóa mềm
     */
    @Transactional
    public AdminUserDetailDto restoreUser(Integer userId, Integer adminId) {
        log.info("Admin {} restoring user ID: {}", adminId, userId);

        User user = findUserOrThrow(userId);
        if (user.getDeletedAt() == null) {
            throw new BadRequestException("User is not deleted");
        }

        user.setDeletedAt(null);
        user.setIsActive(true);
        User restoredUser = userRepository.save(user);

        publishEvent(userId, restoredUser.getEmail(), UserAction.RESTORED, adminId, "Admin restored user");

        log.info("User {} restored successfully", userId);
        return adminUserMapper.toDetailDto(restoredUser);
    }

    // ============ Ban / Unban ============

    /**
     * Khóa tài khoản user (Ban/Deactivate)
     */
    @Transactional
    public void banUser(Integer userId, AdminBanUserRequest request, Integer adminId) {
        log.info("Admin {} banning user ID: {} - Reason: {}", adminId, userId, request.getReason());

        User user = findActiveUserOrThrow(userId);
        preventSelfAction(userId, adminId, "ban");

        user.setIsActive(false);

        // Set lock duration
        if (request.getLockDurationHours() != null && request.getLockDurationHours() > 0) {
            user.setLockedUntil(Instant.now().plus(request.getLockDurationHours(), ChronoUnit.HOURS));
        } else {
            // Khóa vĩnh viễn: set lockedUntil rất xa trong tương lai
            user.setLockedUntil(Instant.now().plus(365 * 100, ChronoUnit.DAYS));
        }

        userRepository.save(user);

        // Event → SessionEventListener sẽ tự revoke toàn bộ sessions
        publishEvent(userId, user.getEmail(), UserAction.BANNED, adminId, request.getReason());

        log.info("User {} banned successfully", userId);
    }

    /**
     * Mở khóa tài khoản user
     */
    @Transactional
    public AdminUserDetailDto unbanUser(Integer userId, Integer adminId) {
        log.info("Admin {} unbanning user ID: {}", adminId, userId);

        User user = findUserOrThrow(userId);

        user.setIsActive(true);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        User unbannedUser = userRepository.save(user);

        publishEvent(userId, unbannedUser.getEmail(), UserAction.ACTIVATED, adminId, "Admin unbanned user");

        log.info("User {} unbanned successfully", userId);
        return adminUserMapper.toDetailDto(unbannedUser);
    }

    // ============ Role Management ============

    /**
     * Đổi role của user
     */
    @Transactional
    public AdminUserDetailDto changeRole(Integer userId, AdminChangeRoleRequest request, Integer adminId) {
        log.info("Admin {} changing role of user ID: {} to {}", adminId, userId, request.getRole());

        User user = findActiveUserOrThrow(userId);
        preventSelfAction(userId, adminId, "change role of");

        Role oldRole = user.getRole();
        user.setRole(request.getRole());
        User updatedUser = userRepository.save(user);

        String reason = String.format("Role changed from %s to %s. Reason: %s",
                oldRole, request.getRole(), request.getReason());
        publishEvent(userId, updatedUser.getEmail(), UserAction.ROLE_CHANGED, adminId, reason);

        log.info("User {} role changed from {} to {}", userId, oldRole, request.getRole());
        return adminUserMapper.toDetailDto(updatedUser);
    }

    // ============ Reset Password ============

    /**
     * Admin reset password cho user
     */
    @Transactional
    public void resetPassword(Integer userId, AdminResetPasswordRequest request, Integer adminId) {
        log.info("Admin {} resetting password for user ID: {}", adminId, userId);

        User user = findActiveUserOrThrow(userId);

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        // Event → SessionEventListener sẽ tự revoke toàn bộ sessions (buộc đăng nhập lại)
        publishEvent(userId, user.getEmail(), UserAction.PASSWORD_RESET, adminId, "Admin reset password");

        log.info("Password reset successfully for user {}", userId);
    }

    // ============ Session Management ============

    /**
     * Xem danh sách sessions (refresh tokens) của user
     */
    @Transactional(readOnly = true)
    public List<UserSessionDto> getUserSessions(Integer userId) {
        log.info("Admin fetching sessions for user ID: {}", userId);
        findUserOrThrow(userId); // Validate user exists

        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdOrderByLastUsedAtDesc(userId);
        return tokens.stream()
                .map(token -> UserSessionDto.builder()
                        .tokenId(token.getId())
                        .ipAddress(token.getIpAddress())
                        .userAgent(token.getUserAgent())
                        .lastUsedAt(token.getLastUsedAt())
                        .createdAt(token.getCreatedAt())
                        .expiryDate(token.getExpiryDate())
                        .revoked(token.getRevoked())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Thu hồi một session cụ thể
     */
    @Transactional
    public void revokeSession(Integer userId, Long tokenId) {
        log.info("Admin revoking session {} for user {}", tokenId, userId);
        findUserOrThrow(userId);
        refreshTokenRepository.deleteByUserIdAndTokenId(userId, tokenId);
    }

    /**
     * Thu hồi tất cả sessions của user
     */
    @Transactional
    public void revokeAllSessions(Integer userId, Integer adminId) {
        log.info("Admin {} revoking all sessions for user {}", adminId, userId);
        findUserOrThrow(userId);
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ============ Dashboard ============

    /**
     * Lấy dữ liệu Dashboard Overview
     */
    @Transactional(readOnly = true)
    public AdminDashboardDto getDashboard() {
        log.info("Admin fetching dashboard data");

        long totalUsers = userRepository.countByDeletedAtIsNull();
        long activeUsers = userRepository.countByIsActiveAndDeletedAtIsNull(true);
        long inactiveUsers = userRepository.countByIsActiveAndDeletedAtIsNull(false);
        long deletedUsers = userRepository.countByDeletedAtIsNotNull();

        // Tính % 
        double activePercentage = totalUsers > 0 ? (activeUsers * 100.0) / totalUsers : 0;
        double bannedPercentage = totalUsers > 0 ? (inactiveUsers * 100.0) / totalUsers : 0;

        // Role distribution
        Map<String, Long> roleDistribution = new LinkedHashMap<>();
        for (Role role : Role.values()) {
            roleDistribution.put(role.name(), userRepository.countByRoleAndDeletedAtIsNull(role));
        }

        // Daily registrations (30 ngày gần nhất)
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<Object[]> dailyRaw = userRepository.countDailyRegistrations(thirtyDaysAgo);
        List<AdminDashboardDto.DailyRegistrationStat> dailyStats = dailyRaw.stream()
                .map(row -> AdminDashboardDto.DailyRegistrationStat.builder()
                        .date(row[0].toString())
                        .count(((Number) row[1]).longValue())
                        .build())
                .collect(Collectors.toList());

        // Monthly registrations (12 tháng gần nhất)
        LocalDateTime twelveMonthsAgo = LocalDateTime.now().minusMonths(12);
        List<Object[]> monthlyRaw = userRepository.countMonthlyRegistrations(twelveMonthsAgo);
        List<AdminDashboardDto.MonthlyRegistrationStat> monthlyStats = monthlyRaw.stream()
                .map(row -> AdminDashboardDto.MonthlyRegistrationStat.builder()
                        .year(((Number) row[0]).intValue())
                        .month(((Number) row[1]).intValue())
                        .count(((Number) row[2]).longValue())
                        .build())
                .collect(Collectors.toList());

        // New users today / this month
        long newUsersToday = dailyStats.stream()
                .filter(s -> s.getDate().equals(LocalDate.now().toString()))
                .mapToLong(AdminDashboardDto.DailyRegistrationStat::getCount)
                .sum();

        long newUsersThisMonth = monthlyStats.stream()
                .filter(s -> s.getYear() == LocalDate.now().getYear()
                        && s.getMonth() == LocalDate.now().getMonthValue())
                .mapToLong(AdminDashboardDto.MonthlyRegistrationStat::getCount)
                .sum();

        return AdminDashboardDto.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .inactiveUsers(inactiveUsers)
                .deletedUsers(deletedUsers)
                .newUsersToday(newUsersToday)
                .newUsersThisMonth(newUsersThisMonth)
                .activePercentage(Math.round(activePercentage * 100.0) / 100.0)
                .bannedPercentage(Math.round(bannedPercentage * 100.0) / 100.0)
                .roleDistribution(roleDistribution)
                .dailyRegistrations(dailyStats)
                .monthlyRegistrations(monthlyStats)
                .build();
    }

    // ============ Private Helper Methods ============

    private User findUserOrThrow(Integer userId) {
        return userRepository.findByIdIncludeDeleted(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User with ID " + userId));
    }

    private User findActiveUserOrThrow(Integer userId) {
        User user = findUserOrThrow(userId);
        if (user.getDeletedAt() != null) {
            throw new BadRequestException("User has been deleted. Restore first before performing this action.");
        }
        return user;
    }

    /**
     * Ngăn admin tự thao tác trên chính mình (ban, delete, change role)
     */
    private void preventSelfAction(Integer targetUserId, Integer adminId, String action) {
        if (targetUserId.equals(adminId)) {
            throw new BadRequestException("You cannot " + action + " yourself");
        }
    }

    private void publishEvent(Integer userId, String email, UserAction action,
                               Integer adminId, String reason) {
        eventPublisher.publishEvent(
                new UserStatusChangedEvent(this, userId, email, action, adminId, reason)
        );
    }

    private Sort buildSort(String sortBy, String sortDirection) {
        // Whitelist các fields được phép sort
        Set<String> allowedSortFields = Set.of(
                "createdAt", "updatedAt", "fullName", "email", "role",
                "isActive", "lastLoginAt", "loyaltyPoints", "totalSpent"
        );

        if (sortBy == null || !allowedSortFields.contains(sortBy)) {
            sortBy = "createdAt";
        }

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDirection)
                ? Sort.Direction.ASC : Sort.Direction.DESC;

        return Sort.by(direction, sortBy);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("[\\s-]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }
}
