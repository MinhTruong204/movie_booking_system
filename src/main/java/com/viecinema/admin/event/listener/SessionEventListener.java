package com.viecinema.admin.event.listener;

import com.viecinema.admin.event.UserAction;
import com.viecinema.admin.event.UserStatusChangedEvent;
import com.viecinema.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener tự động revoke refresh tokens khi user bị ban/delete/reset password.
 * Đây là Observer Pattern: Khi AdminUserService phát sự kiện,
 * listener này tự "nghe" và xử lý mà service KHÔNG cần biết.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventListener {

    private final RefreshTokenRepository refreshTokenRepository;

    @EventListener
    @Transactional
    public void onUserStatusChanged(UserStatusChangedEvent event) {
        UserAction action = event.getAction();

        // Chỉ revoke sessions cho các hành động ảnh hưởng đến quyền truy cập
        if (action == UserAction.BANNED
                || action == UserAction.SOFT_DELETED
                || action == UserAction.PASSWORD_RESET) {

            log.info("[SessionEventListener] Revoking all sessions for user {} due to action: {}",
                    event.getUserId(), action);

            refreshTokenRepository.revokeAllByUserId(event.getUserId());

            log.info("[SessionEventListener] All sessions revoked for user {}", event.getUserId());
        }
    }
}
