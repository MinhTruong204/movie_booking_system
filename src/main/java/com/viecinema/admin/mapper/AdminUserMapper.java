package com.viecinema.admin.mapper;

import com.viecinema.admin.dto.AdminUserDetailDto;
import com.viecinema.admin.dto.AdminUserListDto;
import com.viecinema.admin.dto.request.AdminCreateUserRequest;
import com.viecinema.auth.entity.User;
import org.mapstruct.*;

/**
 * MapStruct mapper cho Admin User DTOs.
 * Tự động convert giữa Entity và DTOs.
 */
@Mapper(componentModel = "spring")
public interface AdminUserMapper {

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "gender", expression = "java(user.getGender() != null ? user.getGender().name() : null)")
    AdminUserListDto toListDto(User user);

    @Mapping(target = "userId", source = "id")
    @Mapping(target = "role", expression = "java(user.getRole() != null ? user.getRole().name() : null)")
    @Mapping(target = "gender", expression = "java(user.getGender() != null ? user.getGender().name() : null)")
    @Mapping(target = "membershipTierName",
            expression = "java(user.getMembershipTier() != null ? user.getMembershipTier().getName() : null)")
    @Mapping(target = "membershipTierId",
            expression = "java(user.getMembershipTier() != null ? user.getMembershipTier().getId() : null)")
    AdminUserDetailDto toDetailDto(User user);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "membershipTier", ignore = true)
    @Mapping(target = "loyaltyPoints", ignore = true)
    @Mapping(target = "totalSpent", ignore = true)
    @Mapping(target = "memberSince", ignore = true)
    @Mapping(target = "phoneVerified", ignore = true)
    @Mapping(target = "lastLoginAt", ignore = true)
    @Mapping(target = "failedLoginAttempts", ignore = true)
    @Mapping(target = "lockedUntil", ignore = true)
    User createRequestToUser(AdminCreateUserRequest request);
}
