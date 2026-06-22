package com.viecinema.admin.service;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;
import com.viecinema.admin.dto.ImportResult;
import com.viecinema.admin.dto.request.UserSearchCriteria;
import com.viecinema.admin.event.UserAction;
import com.viecinema.admin.event.UserStatusChangedEvent;
import com.viecinema.admin.specification.UserSpecification;
import com.viecinema.auth.entity.User;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.enums.Gender;
import com.viecinema.common.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý Import/Export danh sách user.
 * Hỗ trợ cả CSV và Excel (.xlsx).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExportService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;

    private static final String[] EXPORT_HEADERS = {
            "User ID", "Full Name", "Email", "Phone", "Role", "Gender",
            "Birth Date", "Is Active", "Email Verified", "Phone Verified",
            "Loyalty Points", "Total Spent", "Member Since",
            "Last Login At", "Created At"
    };

    private static final String[] IMPORT_HEADERS = {
            "Full Name", "Email", "Phone", "Password", "Role", "Gender", "Birth Date"
    };

    // ============ Export ============

    /**
     * Export danh sách user ra CSV (áp dụng filter từ criteria)
     */
    @Transactional(readOnly = true)
    public ByteArrayOutputStream exportUsersToCSV(UserSearchCriteria criteria) throws IOException {
        log.info("Exporting users to CSV");

        List<User> users = getFilteredUsers(criteria);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            // Header
            writer.writeNext(EXPORT_HEADERS);

            // Data rows
            for (User user : users) {
                writer.writeNext(userToRow(user));
            }
        }

        log.info("Exported {} users to CSV", users.size());
        return outputStream;
    }

    /**
     * Export danh sách user ra Excel (.xlsx)
     */
    @Transactional(readOnly = true)
    public ByteArrayOutputStream exportUsersToExcel(UserSearchCriteria criteria) throws IOException {
        log.info("Exporting users to Excel");

        List<User> users = getFilteredUsers(criteria);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Users");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(EXPORT_HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            for (int i = 0; i < users.size(); i++) {
                Row row = sheet.createRow(i + 1);
                String[] values = userToRow(users.get(i));
                for (int j = 0; j < values.length; j++) {
                    row.createCell(j).setCellValue(values[j] != null ? values[j] : "");
                }
            }

            // Auto-size columns
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(outputStream);
        }

        log.info("Exported {} users to Excel", users.size());
        return outputStream;
    }

    /**
     * Tải template import (CSV)
     */
    public ByteArrayOutputStream getImportTemplate() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(outputStream))) {
            writer.writeNext(IMPORT_HEADERS);
            // Example row
            writer.writeNext(new String[]{
                    "Nguyen Van A", "example@email.com", "0901234567",
                    "Password@123", "CUSTOMER", "MALE", "1990-01-15"
            });
        }

        return outputStream;
    }

    // ============ Import ============

    /**
     * Import user từ file CSV
     */
    @Transactional
    public ImportResult importUsersFromCSV(MultipartFile file, Integer adminId) throws IOException {
        log.info("Admin {} importing users from CSV", adminId);

        ImportResult result = ImportResult.builder()
                .errors(new ArrayList<>())
                .build();

        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] header = reader.readNext(); // Skip header row
            if (header == null) {
                throw new IOException("Empty file");
            }

            String[] line;
            int rowNum = 1;
            int successCount = 0;
            int failedCount = 0;

            while ((line = reader.readNext()) != null) {
                rowNum++;
                try {
                    processImportRow(line, rowNum, result);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    result.getErrors().add(ImportResult.ImportError.builder()
                            .row(rowNum)
                            .field("general")
                            .message(e.getMessage())
                            .build());
                }
            }

            result.setTotalRows(rowNum - 1);
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);

        } catch (CsvValidationException e) {
            throw new IOException("Invalid CSV format: " + e.getMessage());
        }

        // Publish event for each imported user
        if (result.getSuccessCount() > 0) {
            log.info("Import completed: {} success, {} failed", result.getSuccessCount(), result.getFailedCount());
        }

        return result;
    }

    /**
     * Import user từ file Excel (.xlsx)
     */
    @Transactional
    public ImportResult importUsersFromExcel(MultipartFile file, Integer adminId) throws IOException {
        log.info("Admin {} importing users from Excel", adminId);

        ImportResult result = ImportResult.builder()
                .errors(new ArrayList<>())
                .build();

        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int totalRows = sheet.getLastRowNum(); // Exclude header
            int successCount = 0;
            int failedCount = 0;

            for (int i = 1; i <= totalRows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String[] values = new String[IMPORT_HEADERS.length];
                    for (int j = 0; j < IMPORT_HEADERS.length; j++) {
                        Cell cell = row.getCell(j);
                        values[j] = getCellStringValue(cell);
                    }
                    processImportRow(values, i + 1, result);
                    successCount++;
                } catch (Exception e) {
                    failedCount++;
                    result.getErrors().add(ImportResult.ImportError.builder()
                            .row(i + 1)
                            .field("general")
                            .message(e.getMessage())
                            .build());
                }
            }

            result.setTotalRows(totalRows);
            result.setSuccessCount(successCount);
            result.setFailedCount(failedCount);
        }

        log.info("Excel import completed: {} success, {} failed",
                result.getSuccessCount(), result.getFailedCount());
        return result;
    }

    // ============ Private Helper Methods ============

    private List<User> getFilteredUsers(UserSearchCriteria criteria) {
        if (criteria == null) {
            criteria = new UserSearchCriteria();
        }
        Specification<User> spec = UserSpecification.buildFromCriteria(criteria);
        return userRepository.findAll(spec);
    }

    private String[] userToRow(User user) {
        return new String[]{
                String.valueOf(user.getId()),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() != null ? user.getRole().name() : "",
                user.getGender() != null ? user.getGender().name() : "",
                user.getBirthDate() != null ? user.getBirthDate().toString() : "",
                String.valueOf(user.getIsActive()),
                String.valueOf(user.getEmailVerified()),
                String.valueOf(user.getPhoneVerified()),
                String.valueOf(user.getLoyaltyPoints()),
                user.getTotalSpent() != null ? user.getTotalSpent().toString() : "0",
                user.getMemberSince() != null ? user.getMemberSince().toString() : "",
                user.getLastLoginAt() != null ? user.getLastLoginAt().toString() : "",
                user.getCreatedAt() != null ? user.getCreatedAt().toString() : ""
        };
    }

    /**
     * Xử lý một dòng import
     * Thứ tự: Full Name, Email, Phone, Password, Role, Gender, Birth Date
     */
    private void processImportRow(String[] values, int rowNum, ImportResult result) {
        if (values.length < 4) {
            throw new IllegalArgumentException("Row must have at least 4 columns (Full Name, Email, Phone, Password)");
        }

        String fullName = getValueOrNull(values, 0);
        String email = getValueOrNull(values, 1);
        String phone = getValueOrNull(values, 2);
        String password = getValueOrNull(values, 3);
        String roleStr = getValueOrNull(values, 4);
        String genderStr = getValueOrNull(values, 5);
        String birthDateStr = getValueOrNull(values, 6);

        // Validate required fields
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full Name is required");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required");
        }
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Normalize
        email = email.toLowerCase().trim();

        // Check duplicate
        if (userRepository.existsByEmailAndDeletedAtIsNull(email)) {
            result.getErrors().add(ImportResult.ImportError.builder()
                    .row(rowNum)
                    .field("email")
                    .message("Email already exists: " + email)
                    .build());
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // Build user
        User.UserBuilder builder = User.builder()
                .fullName(fullName.trim())
                .email(email)
                .phone(phone != null ? phone.trim() : null)
                .passwordHash(passwordEncoder.encode(password))
                .emailVerified(true);

        // Parse role
        if (roleStr != null && !roleStr.isBlank()) {
            try {
                builder.role(Role.valueOf(roleStr.toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                builder.role(Role.CUSTOMER);
            }
        }

        // Parse gender
        if (genderStr != null && !genderStr.isBlank()) {
            try {
                builder.gender(Gender.valueOf(genderStr.toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                // Ignore invalid gender
            }
        }

        // Parse birth date
        if (birthDateStr != null && !birthDateStr.isBlank()) {
            try {
                builder.birthDate(LocalDate.parse(birthDateStr.trim()));
            } catch (DateTimeParseException e) {
                // Ignore invalid date
            }
        }

        userRepository.save(builder.build());
    }

    private String getValueOrNull(String[] values, int index) {
        if (index >= values.length) return null;
        String value = values[index];
        return (value != null && !value.isBlank()) ? value.trim() : null;
    }

    private String getCellStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                // Tránh trả về dạng scientific notation cho số
                yield String.valueOf((long) cell.getNumericCellValue());
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }
}
