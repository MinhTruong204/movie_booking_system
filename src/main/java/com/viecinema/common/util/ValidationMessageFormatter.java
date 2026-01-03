package com.viecinema.common.util;

import lombok.experimental.UtilityClass;
import org.springframework.validation.FieldError;

@UtilityClass
public class ValidationMessageFormatter {
    public static String formatValidationMessage(FieldError fieldError) {
        String key = switch (fieldError.getCode()) {
            case "NotNull", "NotBlank" -> "FIELD_REQUIRED";
            case "Size" -> "FIELD_SIZE";
            case "Min", "Max", "Range" -> "FIELD_OUT_OF_RANGE";
            case "Pattern" -> "FIELD_PATTERN_MISMATCH";
            case null, default -> null;
        };

//        return switch (key) {
//            case "FIELD_REQUIRED" -> SimpleApiMessage.FIELD_REQUIRED.format(fieldError.getField());
//            case "FIELD_SIZE" -> {
//
//
//            };
//        }
        return null;
    }
}
