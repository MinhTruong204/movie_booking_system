package com.viecinema.auth.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.viecinema.auth.entity.User;
import lombok.Getter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
@ToString
public class UserPrincipal implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final Integer id;
    private final String fullName;
    private final String email;

    @JsonIgnore
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    // Một số thông tin phụ trợ (có thể lấy từ entity nếu muốn)
    private final Boolean isActive;
    private final Instant lockedUntil;

    public UserPrincipal(Integer id,
                         String fullName,
                         String email,
                         String password,
                         Collection<? extends GrantedAuthority> authorities,
                         Boolean isActive,
                         Instant lockedUntil) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.isActive = isActive;
        this.lockedUntil = lockedUntil;
    }

    /**
     * Factory method — chuyển từ User entity sang UserPrincipal.
     * Nếu entity của bạn đặt tên trường khác (ví dụ userId), điều chỉnh tương ứng.
     */
    public static UserPrincipal create(User user) {
        // Map role -> GrantedAuthority (ví dụ: role = 'customer' -> ROLE_CUSTOMER)
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

        // Nếu muốn map nhiều role/authority, thay đổi logic trên:
        // e.g., user.getRoles().stream()...

        Instant lockedUntil = null;
        if (user.getLockedUntil() != null) {
            lockedUntil = user.getLockedUntil();
        }

        return new UserPrincipal(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPasswordHash(),
                authorities,
                user.getIsActive(),
                lockedUntil
        );
    }

    // -------------------- UserDetails impl --------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    // username dùng email trong project này
    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    // Đơn giản trả true — nếu muốn kiểm tra expiry, thay logic ở đây
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    // Nếu có trường lockedUntil, có thể kiểm tra ở đây
    @Override
    public boolean isAccountNonLocked() {
        if (lockedUntil == null) return true;
        return Instant.now().isAfter(lockedUntil);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // isEnabled map tới isActive của User
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive);
    }

    // -------------------- equals / hashCode --------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserPrincipal that = (UserPrincipal) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
