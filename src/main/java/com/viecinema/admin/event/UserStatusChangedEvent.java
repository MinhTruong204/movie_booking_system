package com.viecinema.admin.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event phát ra khi trạng thái User thay đổi bởi Admin.
 * <p>
 * Đây là core của Observer Pattern: Service chỉ cần phát event này,
 * các Listener sẽ tự động lắng nghe và xử lý side-effect (revoke session, ghi audit log, ...).
 * </p>
 */
@Getter
public class UserStatusChangedEvent extends ApplicationEvent {

    private final Integer userId;
    private final String userEmail;
    private final UserAction action;
    private final Integer performedByAdminId;
    private final String reason;

    public UserStatusChangedEvent(Object source,
                                  Integer userId,
                                  String userEmail,
                                  UserAction action,
                                  Integer performedByAdminId,
                                  String reason) {
        super(source);
        this.userId = userId;
        this.userEmail = userEmail;
        this.action = action;
        this.performedByAdminId = performedByAdminId;
        this.reason = reason;
    }
}
