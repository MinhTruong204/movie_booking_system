package com.viecinema.common.util;

import com.viecinema.common.exception.LoginRequiredException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import static com.viecinema.common.constant.ErrorMessage.LOGIN_REQUIRED_ERROR;

@Slf4j
@Component
public class AuthUtil {
    public Integer extractUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new LoginRequiredException(LOGIN_REQUIRED_ERROR);
        }
        try {
            return Integer.parseInt(userDetails.getUsername());
        } catch (Exception e) {
            log.debug("Could not extract userId from userDetails: {}", e.getMessage());
            return null;
        }
    }
}
