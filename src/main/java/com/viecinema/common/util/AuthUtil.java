package com.viecinema.common.util;

import com.viecinema.security.CustomUserDetails;
import jakarta.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthUtil {
    public Integer extractUserId(UserDetails userDetails) {
        if (userDetails instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }
        // This should not happen in a normal flow if authentication is set up correctly
        log.error("UserDetails is not an instance of CustomUserDetails. Actual type: {}", userDetails.getClass().getName());
        throw new ValidationException("Unable to extract user ID from principal.");
    }
}
