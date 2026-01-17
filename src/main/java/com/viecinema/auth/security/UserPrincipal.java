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
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
@ToString
public class UserPrincipal implements UserDetails, Serializable {

    private final Integer id;
    private final String fullName;
    private final String email;

    @JsonIgnore
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    private final Boolean isActive;
    private final Instant lockedUntil;
    private final LocalDateTime deletedAt;

    public UserPrincipal(Integer id,
                         String fullName,
                         String email,
                         String password,
                         Collection<? extends GrantedAuthority> authorities,
                         Boolean isActive,
                         Instant lockedUntil,
                         LocalDateTime deletedAt) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.isActive = isActive;
        this.lockedUntil = lockedUntil;
        this.deletedAt = deletedAt;
    }

    public static UserPrincipal create(User user) {
        // Map role -> GrantedAuthority (Ex: role = 'customer' -> ROLE_CUSTOMER)
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole())
        );

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
                lockedUntil,
                user.getDeletedAt()
        );
    }

    // -------------------- UserDetails impl --------------------

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        if (lockedUntil == null) return true;
        return Instant.now().isAfter(lockedUntil);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(isActive) && deletedAt == null;
    }
}
