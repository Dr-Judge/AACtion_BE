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
import com.likelion.drjudge.domain.user.entity.UserStatus;
import com.likelion.drjudge.domain.user.exception.UserErrorCode;
import com.likelion.drjudge.domain.user.repository.UserRepository;
import com.likelion.drjudge.global.exception.BusinessException;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final TokenBlacklistService tokenBlacklistService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.findByLoginId(request.loginId()).isPresent()) {
            throw new BusinessException(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
        }
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
        }

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = User.createLocalUser(
                request.loginId(), encodedPassword, request.email(), request.name(), request.nickname());

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause().getMessage();
            if (msg != null && msg.contains("uq_users_login_id")) {
                throw new BusinessException(UserErrorCode.LOGIN_ID_ALREADY_EXISTS);
            }
            if (msg != null && msg.contains("uq_users_email")) {
                throw new BusinessException(UserErrorCode.EMAIL_ALREADY_EXISTS);
            }
            throw new BusinessException(CommonErrorCode.INVALID_INPUT_VALUE);
        }

        return new SignupResponse(user.getId());
    }

    public TokenResponse login(LoginRequest request) {
        try {
            var authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.loginId(), request.password())
            );
            CustomUserPrincipal principal = (CustomUserPrincipal) authentication.getPrincipal();
            return issueTokens(principal.getId());
        } catch (DisabledException e) {
            throw new BusinessException(UserErrorCode.ALREADY_WITHDRAWN);
        } catch (BadCredentialsException | UsernameNotFoundException e) {
            throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
        }
    }

    public TokenResponse reissue(String refreshToken) {
        Claims claims = jwtTokenProvider.resolveRefreshClaims(refreshToken);
        if (claims == null) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.extractUserId(claims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.ALREADY_WITHDRAWN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        Duration ttl = Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs());

        boolean rotated = refreshTokenService.rotate(userId, refreshToken, newRefreshToken, ttl);
        if (!rotated) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        return new TokenResponse(newAccessToken, newRefreshToken);
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