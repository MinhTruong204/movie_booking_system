package com.viecinema.auth.service;

import com.viecinema.auth.dto.request.LoginRequest;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.response.LoginResponse;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.entity.User;
import com.viecinema.auth.mapper.UserMapper;
import com.viecinema.auth.repository.MembershipTierRepository;
import com.viecinema.auth.repository.RefreshTokenRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.constant.ApiMessage;
import com.viecinema.common.enums.TokenType;
import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.BusinessException;
import com.viecinema.common.exception.DuplicateResourceException;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.validation.validator.UserValidator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static com.viecinema.common.constant.ErrorMessage.ACCOUNT_DISABLE_ERROR;
import static com.viecinema.common.constant.ErrorMessage.ACCOUNT_LOCKED_ERROR;

@Service
@RequiredArgsConstructor
@Builder
public class AuthService {

    private static final int EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsServiceIml;
    private final UserMapper userMapper;
    private final UserValidator userValidator;
    private final RefreshTokenService refreshTokenService;

    //    Register
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        try {
            validateUniqueConstraints(request);
            User user = userMapper.registerRequestToUser(request);
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            user.setEmailVerified(true);
            userRepository.save(user);

            return userMapper.toRegisterResponse(user);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ApiMessage.FIELD_ALREADY_EXISTS, e.getMessage());
        }
    }

    //  Login
    @Transactional
    public LoginResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        String email = loginRequest.getEmail();
        String password = loginRequest.getPassword();

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        userValidator.validateUser(user);

        try {
            // Check email and password
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(email,null);
            String refreshToken = refreshTokenService.createAndSaveRefreshToken(user.getId(),user.getEmail(), ipAddress, userAgent);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .fullName(user.getFullName())
                    .expiresIn(jwtService.extractExpiration(accessToken,TokenType.ACCESS).getTime())
                    .build();
        } catch (LockedException e) {
            throw new BadRequestException(ACCOUNT_LOCKED_ERROR);
        } catch (DisabledException e) {
            throw new BadRequestException(ACCOUNT_DISABLE_ERROR);
        }

    }

    // ============ Private Helper Methods ============

    private void validateUniqueConstraints(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhone = normalizePhone(request.getPhone());

        if (userRepository.existsByEmailAndDeletedAtIsNull(normalizedEmail))
            throw new DuplicateResourceException("Email");
        if (normalizedPhone != null &&
                userRepository.existsByPhoneAndDeletedAtIsNull(normalizedPhone))
            throw new DuplicateResourceException("Phone");
    }

    private String normalizePhone(String phone) {
        if (phone == null) return null;
        phone = phone.replaceAll("[\\s-]", "");
        if (phone.startsWith("+84")) {
            phone = "0" + phone.substring(3);
        }
        return phone;
    }

    private String normalizeEmail(String email) {
        return email.toLowerCase();
    }
}
