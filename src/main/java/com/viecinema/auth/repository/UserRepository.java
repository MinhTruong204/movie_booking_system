package com.viecinema.auth.repository;

import com.viecinema.auth.entity.User;
import com.viecinema.common.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmailAndDeletedAtIsNull(String email);

    Boolean existsByEmailAndDeletedAtIsNull(String email);

    Boolean existsByPhoneAndDeletedAtIsNull(String phone);

    @Query("SELECT u FROM User u " +
            "LEFT JOIN FETCH u.membershipTier " +
            "WHERE u.id = :userId " +
            "AND u.deletedAt IS NULL " +
            "AND u.isActive = true")
    Optional<User> findActiveUserWithMembership(@Param("userId") Integer userId);

    // ============ Admin Dashboard Statistics ============

    long countByDeletedAtIsNull();

    long countByIsActiveAndDeletedAtIsNull(Boolean isActive);

    long countByDeletedAtIsNotNull();

    long countByRoleAndDeletedAtIsNull(Role role);

    @Query("SELECT FUNCTION('DATE', u.createdAt) as date, COUNT(u) as count FROM User u " +
            "WHERE u.createdAt >= :from GROUP BY FUNCTION('DATE', u.createdAt) ORDER BY date")
    List<Object[]> countDailyRegistrations(@Param("from") LocalDateTime from);

    @Query("SELECT YEAR(u.createdAt), MONTH(u.createdAt), COUNT(u) FROM User u " +
            "WHERE u.createdAt >= :from GROUP BY YEAR(u.createdAt), MONTH(u.createdAt) " +
            "ORDER BY YEAR(u.createdAt), MONTH(u.createdAt)")
    List<Object[]> countMonthlyRegistrations(@Param("from") LocalDateTime from);

    // ============ Admin User Lookup ============

    /**
     * Tìm user kể cả đã soft delete (cho admin restore)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.membershipTier WHERE u.id = :userId")
    Optional<User> findByIdIncludeDeleted(@Param("userId") Integer userId);

    /**
     * Kiểm tra email trùng (loại trừ user hiện tại, dùng khi update)
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
            "WHERE u.email = :email AND u.id <> :excludeUserId AND u.deletedAt IS NULL")
    Boolean existsByEmailAndIdNotAndDeletedAtIsNull(@Param("email") String email,
                                                    @Param("excludeUserId") Integer excludeUserId);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
            "WHERE u.phone = :phone AND u.id <> :excludeUserId AND u.deletedAt IS NULL")
    Boolean existsByPhoneAndIdNotAndDeletedAtIsNull(@Param("phone") String phone,
                                                    @Param("excludeUserId") Integer excludeUserId);

    @Query("SELECT u FROM User u WHERE u.email = :email")
    User findByEmail(@Param("email") String email);
}
