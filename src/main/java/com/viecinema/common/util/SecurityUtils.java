package com.viecinema.common.util;

import com.viecinema.auth.security.UserPrincipal;
import com.viecinema.common.exception.LoginRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import static com.viecinema.common.constant.ErrorMessage.LOGIN_REQUIRED_ERROR;

@Slf4j
@Component
public class SecurityUtils {
    public boolean userHasAdminRole(UserPrincipal userPrincipal) {
        return userPrincipal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
