package com.likelion.drjudge.domain.auth.service;

import com.likelion.drjudge.domain.auth.dto.request.LoginRequest;
import com.likelion.drjudge.domain.auth.dto.request.SignupRequest;
import com.likelion.drjudge.domain.auth.dto.response.SignupResponse;
import com.likelion.drjudge.domain.auth.dto.response.TokenResponse;
import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.domain.jwt.jwt.JwtTokenProvider;
import com.likelion.drjudge.domain.jwt.service.CustomUserPrincipal;
import com.likelion.drjudge.domain.jwt.service.RefreshTokenService;
import com.likelion.drjudge.domain.jwt.service.TokenBlacklistService;
import com.likelion.drjudge.domain.user.entity.User;
import com.likelion.drjudge.domain.user.exception.UserErrorCode;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    public SignupResponse signup(SignupRequest request) {
        if (userRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new BusinessException(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }

        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        User user = User.createLocalUser(
                request.loginId(), encodedPassword, request.email(), request.name(), request.nickname());

        userRepository.save(user);

        return new SignupResponse(user.getId());
    }

    public TokenResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
            );
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            return issueTokens(principal.getId());
        } catch (AuthenticationException e) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    public TokenResponse reissue(String refreshToken) {
        Claims claims = jwtTokenProvider.resolveRefreshClaims(refreshToken);
        if (claims == null) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.extractUserId(claims);
        if (!refreshTokenService.isValid(userId, refreshToken)) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return issueTokens(userId);
    }

    public void logout(Long userId, Claims accessClaims) {
        String jti = jwtTokenProvider.extractJti(accessClaims);
        long remainingMs = jwtTokenProvider.getRemainingValidityMs(accessClaims);
        tokenBlacklistService.blacklist(jti, remainingMs);
        refreshTokenService.delete(userId);
    }

    private TokenResponse issueTokens(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenService.save(
                userId, refreshToken, Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()));

        return new TokenResponse(accessToken, refreshToken);
    }
}