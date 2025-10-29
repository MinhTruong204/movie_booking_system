package com.viecinema.validation.validator;

import com.viecinema.constant.ApiMessage;
import com.viecinema.constant.ValidationConstant;
import com.viecinema.validation.annotation.ValidEmail;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {

    private static final String emailRegex = ValidationConstant.EMAIL_REGEX;
    private static final Pattern pattern = Pattern.compile(emailRegex);

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {

//        Null , Blank
        if(email == null || email.isBlank()) return false;

//        Format
        if(!pattern.matcher(email).matches()) return false;

//        Length
        if(email.length() > ValidationConstant.EMAIL_MAX_LENGTH) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                        ApiMessage.FIELD_TOO_LONG.format("Email", ValidationConstant.EMAIL_MAX_LENGTH))
                    .addConstraintViolation();
            return false;
        }
    return true;

    }

}
