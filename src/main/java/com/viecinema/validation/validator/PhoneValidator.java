package com.viecinema.validation.validator;

import com.viecinema.constant.ApiMessage;
import com.viecinema.constant.ValidationConstant;
import com.viecinema.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;

import java.util.regex.Pattern;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private static final String phoneRegex = ValidationConstant.PHONE_REGEX;
    private static final Pattern pattern = Pattern.compile(phoneRegex);

    @Override
    public boolean isValid(String phone, jakarta.validation.ConstraintValidatorContext context) {
        if(phone == null || phone.isBlank()) return false;

//        Remove spaces and "-"
        String phoneNormalized = phone.replaceAll("[\\s-]", "");

        if(!pattern.matcher(phoneNormalized).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                        ApiMessage.FIELD_INVALID.format("Phone"))
                    .addConstraintViolation();
            return false;
        }
        return true;
    }


}
