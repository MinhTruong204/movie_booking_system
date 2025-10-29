package com.viecinema.auth.user;

import com.viecinema.auth.repository.MembershipTierRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Builder
public class AuthService {

    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final PasswordEncoder passwordEncoder;

    private static final int EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS = 24;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        try {
            validateUniqueConstraints(request);
            User user = buildUserFromRegisterRequest(request);
            userRepository.save(user);
            RegisterResponse userResponse = buildRegisterResponseFromUser(user);
            return userResponse;
        }
        catch (DataIntegrityViolationException e) {
            throw new BusinessException(ApiMessage.FIELD_ALREADY_EXISTS, e.getMessage());
        }
    }

    // ============ Private Helper Methods ============

    private void validateUniqueConstraints(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhone = normalizePhone(request.getPhone());

        if(userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException(ApiMessage.DUPLICATE_EMAIL);
        }

        if(userRepository.existsByPhone(normalizedPhone))
            throw new BusinessException(ApiMessage.DUPLICATE_PHONE);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("[\\s-]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }

    private User buildUserFromRegisterRequest(RegisterRequest request) {
        return User.builder()
                .fullName(request.getFullName())
                .email(normalizeEmail(request.getEmail()))
                .phone(normalizePhone(request.getPhone()))
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();
    }

    private RegisterResponse buildRegisterResponseFromUser(User user) {
        return RegisterResponse.builder()
                .email(user.getEmail())
                .fullName(user.getFullName())
                .build();
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase();
    }
}
