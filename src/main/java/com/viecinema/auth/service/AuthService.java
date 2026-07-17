package com.viecinema.auth.service;

import com.viecinema.auth.dto.request.LoginRequest;
import com.viecinema.auth.dto.request.RegisterRequest;
import com.viecinema.auth.dto.response.LoginResponse;
import com.viecinema.auth.dto.response.RegisterResponse;
import com.viecinema.auth.entity.EmailVerification;
import com.viecinema.auth.entity.User;
import com.viecinema.auth.mapper.UserMapper;
import com.viecinema.auth.repository.EmailVerificationRepository;
import com.viecinema.auth.repository.MembershipTierRepository;
import com.viecinema.auth.repository.RefreshTokenRepository;
import com.viecinema.auth.repository.UserRepository;
import com.viecinema.common.enums.Role;
import com.viecinema.common.enums.TokenType;
import com.viecinema.common.exception.BadRequestException;
import com.viecinema.common.exception.DuplicateResourceException;
import com.viecinema.common.exception.ResourceNotFoundException;
import com.viecinema.common.validation.validator.UserValidator;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.viecinema.common.constant.ErrorMessage.*;

@Service
@RequiredArgsConstructor
@Builder
public class AuthService {

    private static final int EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS = 24;

    private final UserRepository userRepository;
    private final MembershipTierRepository membershipTierRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsServiceIml;
    private final UserMapper userMapper;
    private final UserValidator userValidator;
    private final RefreshTokenService refreshTokenService;
    private final EmailService emailService;

    // ============ Register ============

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        String normalizedPhone = normalizePhone(request.getPhone());

        Optional<User> existingByEmail = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail);

        if (existingByEmail.isPresent()) {
            User existing = existingByEmail.get();
            // Claim: guest chua tung co password -> nang cap thanh tai khoan that
            if (existing.getRole() == Role.GUEST && existing.getPasswordHash() == "guestpass") {
                existing.setFullName(request.getFullName());
                existing.setPhone(normalizedPhone);
                existing.setPasswordHash(passwordEncoder.encode(request.getPassword()));
                existing.setRole(Role.CUSTOMER);
                existing.setEmailVerified(true);
                userRepository.save(existing);
                
                RegisterResponse response = userMapper.toRegisterResponse(existing);
                response.setVerificationRequired(false);
                return response;
            }
            // Da la tai khoan that (co password) -> moi bao trung
            throw new DuplicateResourceException("Email");
        }

        User user = userMapper.registerRequestToUser(request);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEmailVerified(false); // Yeu cau xac thuc email truoc khi login
        userRepository.save(user);

        // Tao token va gui email xac thuc bat dong bo
        String token = createVerificationToken(user.getId());
        emailService.sendVerificationEmail(user, token);

        RegisterResponse response = userMapper.toRegisterResponse(user);
        response.setVerificationRequired(true);
        response.setVerificationSentAt(LocalDateTime.now());
        return response;
    }

    // ============ Verify Email ============

    @Transactional
    public void verifyEmail(String token) {
        EmailVerification verification = emailVerificationRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException(INVALID_VERIFICATION_TOKEN_ERROR));

        // Kiem tra da su dung chua
        if (verification.getIsUsed()) {
            throw new BadRequestException(INVALID_VERIFICATION_TOKEN_ERROR);
        }

        if (verification.isExpired()) {
            throw new BadRequestException(EXPIRED_VERIFICATION_TOKEN_ERROR);
        }

        User user = userRepository.findById(verification.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        user.setEmailVerified(true);
        userRepository.save(user);

        // Danh dau token da su dung (giu lich su thay vi xoa)
        verification.setIsUsed(true);
        verification.setUsedAt(LocalDateTime.now());
        emailVerificationRepository.save(verification);
    }

    // ============ Resend Verification Email ============

    @Transactional
    public void resendVerificationEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmailAndDeletedAtIsNull(normalizedEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }

        // Xoa token cu chua dung (neu co) va tao token moi
        emailVerificationRepository.deleteUnusedByUserId(user.getId());
        String token = createVerificationToken(user.getId());
        emailService.sendVerificationEmail(user, token);
    }

    // ============ Login ============

    @Transactional
    public LoginResponse login(LoginRequest loginRequest, String ipAddress, String userAgent) {
        String email = normalizeEmail(loginRequest.getEmail());
        String password = loginRequest.getPassword();

        User user = userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new ResourceNotFoundException("User"));

        userValidator.validateUser(user);

        // Chan dang nhap neu email chua xac thuc
        if (!user.getEmailVerified()) {
            throw new BadRequestException(EMAIL_NOT_VERIFIED_ERROR);
        }

        try {
            // Check email and password
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(email, null);
            String refreshToken = refreshTokenService.createAndSaveRefreshToken(user.getId(), user.getEmail(), ipAddress, userAgent);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .fullName(user.getFullName())
                    .expiresIn(jwtService.extractExpiration(accessToken, TokenType.ACCESS).getTime())
                    .build();
        } catch (LockedException e) {
            throw new BadRequestException(ACCOUNT_LOCKED_ERROR);
        } catch (DisabledException e) {
            throw new BadRequestException(ACCOUNT_DISABLE_ERROR);
        }
    }

    // ============ Private Helper Methods ============

    private String createVerificationToken(Integer userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        EmailVerification verification = EmailVerification.builder()
                .token(token)
                .userId(userId)
                .tokenType(EmailVerification.TokenType.REGISTRATION)
                .expiresAt(LocalDateTime.now().plusHours(EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS))
                .build();
        emailVerificationRepository.save(verification);
        return token;
    }

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
