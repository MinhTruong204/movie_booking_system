package com.viecinema.admin.specification;

import com.viecinema.admin.dto.request.UserSearchCriteria;
import com.viecinema.auth.entity.User;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builder cho dynamic query từ UserSearchCriteria.
 * <p>
 * Sử dụng Criteria API thay vì viết nhiều query methods trong repository:
 * - Type-safe, compile-time check
 * - Composable: dễ kết hợp nhiều filter
 * - Không cần viết N query methods
 * </p>
 */
public class UserSpecification {

    private UserSpecification() {
        // Utility class
    }

    /**
     * Build Specification từ search criteria.
     * Chỉ áp dụng filter khi field != null.
     */
    public static Specification<User> buildFromCriteria(UserSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search (fullName, email, phone) - dùng LIKE
            if (criteria.getKeyword() != null && !criteria.getKeyword().isBlank()) {
                String keyword = "%" + criteria.getKeyword().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("fullName")), keyword);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), keyword);
                Predicate phoneLike = cb.like(cb.lower(root.get("phone")), keyword);
                predicates.add(cb.or(nameLike, emailLike, phoneLike));
            }

            // Filter by role
            if (criteria.getRole() != null) {
                predicates.add(cb.equal(root.get("role"), criteria.getRole()));
            }

            // Filter by active status
            if (criteria.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), criteria.getIsActive()));
            }

            // Filter by deleted status
            if (criteria.getIsDeleted() != null) {
                if (criteria.getIsDeleted()) {
                    predicates.add(cb.isNotNull(root.get("deletedAt")));
                } else {
                    predicates.add(cb.isNull(root.get("deletedAt")));
                }
            }

            // Filter by email verified
            if (criteria.getEmailVerified() != null) {
                predicates.add(cb.equal(root.get("emailVerified"), criteria.getEmailVerified()));
            }

            // Filter by membership tier
            if (criteria.getMembershipTierId() != null) {
                predicates.add(cb.equal(root.get("membershipTier").get("id"), criteria.getMembershipTierId()));
            }

            // Filter by creation date range
            if (criteria.getCreatedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        criteria.getCreatedFrom().atStartOfDay()));
            }
            if (criteria.getCreatedTo() != null) {
                predicates.add(cb.lessThan(root.get("createdAt"),
                        criteria.getCreatedTo().plusDays(1).atStartOfDay()));
            }

            // Filter by last login date range
            if (criteria.getLastLoginFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("lastLoginAt"),
                        criteria.getLastLoginFrom().atStartOfDay()));
            }
            if (criteria.getLastLoginTo() != null) {
                predicates.add(cb.lessThan(root.get("lastLoginAt"),
                        criteria.getLastLoginTo().plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
