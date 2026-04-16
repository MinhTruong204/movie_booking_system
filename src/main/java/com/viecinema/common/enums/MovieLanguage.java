package com.viecinema.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MovieLanguage {
    VI("vi", "Tiếng Việt"),
    EN("en", "Tiếng Anh");

    private final String code;
    private final String displayName;

    public static MovieLanguage fromCode(String code) {
        for (MovieLanguage lang : MovieLanguage.values()) {
            if (lang.code.equalsIgnoreCase(code)) {
                return lang;
            }
        }
        return null;
    }
}
