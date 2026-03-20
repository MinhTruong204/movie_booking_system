package com.viecinema.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class RequestUtils {
    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";

        if (request != null) {
            remoteAddr = request.getHeader("X-Forwarded-For");
            if (remoteAddr == null || remoteAddr.isEmpty() || "unknown".equalsIgnoreCase(remoteAddr)) {
                remoteAddr = request.getHeader("Proxy-Client-IP");
            }
            if (remoteAddr == null || remoteAddr.isEmpty() || "unknown".equalsIgnoreCase(remoteAddr)) {
                remoteAddr = request.getHeader("WL-Proxy-Client-IP");
            }
            if (remoteAddr == null || remoteAddr.isEmpty() || "unknown".equalsIgnoreCase(remoteAddr)) {
                remoteAddr = request.getRemoteAddr();
            }
        }
        if (remoteAddr != null && remoteAddr.contains(",")) {
            remoteAddr = remoteAddr.split(",")[0];
        }

        return remoteAddr;
    }
    public static String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
