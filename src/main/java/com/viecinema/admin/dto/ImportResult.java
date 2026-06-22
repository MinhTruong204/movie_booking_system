package com.viecinema.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO trả về kết quả import user hàng loạt.
 * Bao gồm thống kê thành công/thất bại và chi tiết lỗi từng dòng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {

    private int totalRows;
    private int successCount;
    private int failedCount;

    @Builder.Default
    private List<ImportError> errors = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImportError {
        private int row;
        private String field;
        private String message;
    }
}
