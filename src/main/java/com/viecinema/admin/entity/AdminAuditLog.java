package com.viecinema.admin.entity;

import com.viecinema.admin.event.UserAction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entity ghi nhận mọi thao tác admin thực hiện trên User.
 * Phục vụ audit trail, truy vết và báo cáo.
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "admin_audit_logs")
public class AdminAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    private UserAction action;

    @Column(name = "target_user_id", nullable = false)
    private Integer targetUserId;

    @Column(name = "performed_by", nullable = false)
    private Integer performedBy;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "details", columnDefinition = "JSON")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false,
            columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
