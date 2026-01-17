package com.viecinema.common.validation.validator;

import com.viecinema.auth.entity.User;
import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.SpecificBusinessException;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static com.viecinema.common.constant.ErrorMessage.ACCOUNT_DISABLE_ERROR;
import static com.viecinema.common.constant.ErrorMessage.ACCOUNT_LOCKED_ERROR;

@Component
public class UserValidator {
    public void validateUser(User user) {
        if (!user.getIsActive()) throw new BadRequestException(ACCOUNT_DISABLE_ERROR);
        if (!user.getEmailVerified()) throw new SpecificBusinessException("Please verify your email before logging in");
        if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(Instant.now()))
            throw new BadRequestException(ACCOUNT_LOCKED_ERROR);
    }
}
