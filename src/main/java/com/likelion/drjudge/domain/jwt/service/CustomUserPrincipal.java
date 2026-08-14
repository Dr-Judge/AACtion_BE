package com.likelion.drjudge.domain.jwt.service;

import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.entity.UserStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserPrincipal implements UserDetails {

    private final Long id;
    private final String loginId;
    private final String password;
    private final boolean enabled;

    private CustomUserPrincipal(
            Long id,
            String loginId,
            String password,
            boolean enabled
    ) {
        this.id = id;
        this.loginId = loginId;
        this.password = password;
        this.enabled = enabled;
    }

    public static CustomUserPrincipal from(User user) {
        return new CustomUserPrincipal(
                user.getId(),
                user.getLoginId(),
                user.getPassword(),
                user.getStatus() == UserStatus.ACTIVE
        );
    }

    public Long getId() { return id; }

    @Override
    public String getUsername() { return loginId; }  // Spring Security 인터페이스 규격상 메서드명은 getUsername() 그대로

    @Override
    public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return enabled; }
}