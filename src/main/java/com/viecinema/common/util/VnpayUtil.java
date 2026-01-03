package com.viecinema.common.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
public class VnpayUtil {

    /**
     * Tạo chuỗi hash HMAC SHA-512
     */
    public static String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Hex.encodeHexString(result);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error generating HMAC SHA-512", e);
            throw new RuntimeException("Error generating hash", e);
        }
    }

    /**
     * Tạo chuỗi hash data từ Map parameters
     * VNPay yêu cầu:  sắp xếp theo alphabet, nối bằng &, URL encode
     */
    public static String hashAllFields(Map<String, String> fields) {
        List<String> fieldNames = new ArrayList<>(fields.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = fields.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                if (itr.hasNext()) {
                    hashData.append('&');
                }
            }
        }

        return hashData.toString();
    }

    /**
     * Tạo query string từ Map
     */
    public static String buildQueryString(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();

        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = params.get(fieldName);

            if (fieldValue != null && !fieldValue.isEmpty()) {
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));

                if (itr.hasNext()) {
                    query.append('&');
                }
            }
        }

        return query.toString();
    }

    /**
     * Parse query string thành Map
     */
    public static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();

        if (queryString == null || queryString.isEmpty()) {
            return params;
        }

        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLEncoder.encode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLEncoder.encode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                params.put(key, value);
            }
        }

        return params;
    }

    /**
     * Lấy IP address từ request
     */
    public static String getIpAddress(jakarta.servlet.http.HttpServletRequest request) {
        String ipAddress = request.getHeader("X-FORWARDED-FOR");

        if (ipAddress == null || ipAddress.isEmpty()) {
            ipAddress = request.getRemoteAddr();
        }

        return ipAddress;
    }

    /**
     * Format datetime theo yêu cầu VNPay:  yyyyMMddHHmmss
     */
    public static String formatDateTime(Date date) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        return formatter.format(date);
    }

    /**
     * Tạo transaction reference (unique)
     */
    public static String generateTxnRef() {
        return String.valueOf(System.currentTimeMillis());
    }
}
