package com.viecinema.admin.event.listener;

import com.viecinema.admin.entity.AdminAuditLog;
import com.viecinema.admin.event.UserStatusChangedEvent;
import com.viecinema.admin.repository.AdminAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener ghi nhận audit log cho mọi thao tác admin trên User.
 * <p>
 * Observer Pattern: Mọi event từ AdminUserService đều được log lại,
 * giúp truy vết và báo cáo mà không cần sửa logic trong service.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AdminAuditLogRepository auditLogRepository;

    @EventListener
    @Transactional
    public void onUserStatusChanged(UserStatusChangedEvent event) {
        log.info("[AuditEventListener] Recording audit log: action={}, userId={}, performedBy={}",
                event.getAction(), event.getUserId(), event.getPerformedByAdminId());

        AdminAuditLog auditLog = AdminAuditLog.builder()
                .action(event.getAction())
                .targetUserId(event.getUserId())
                .performedBy(event.getPerformedByAdminId())
                .reason(event.getReason())
                .build();

        auditLogRepository.save(auditLog);

        log.debug("[AuditEventListener] Audit log saved with ID: {}", auditLog.getId());
    }
}
