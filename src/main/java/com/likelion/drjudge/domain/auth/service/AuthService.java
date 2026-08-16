package com.likelion.drjudge.domain.auth.service;

import com.likelion.drjudge.domain.auth.dto.request.LoginRequest;
import com.likelion.drjudge.domain.auth.dto.request.SignupRequest;
import com.likelion.drjudge.domain.auth.dto.response.SignupResponse;
import com.likelion.drjudge.domain.auth.dto.response.TokenResponse;
import com.likelion.drjudge.domain.auth.dto.response.WithdrawResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int TOKEN_INVALIDATION_MAX_ATTEMPTS = 3;
    private static final long TOKEN_INVALIDATION_RETRY_BACKOFF_MS = 100L;

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

    public TokenResponse reissue(String refreshToken, String oldAccessToken) {
        Claims refreshClaims = jwtTokenProvider.resolveRefreshClaims(refreshToken);
        if (refreshClaims == null) {
            throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        Long userId = jwtTokenProvider.extractUserId(refreshClaims);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(UserErrorCode.ALREADY_WITHDRAWN);
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
        Duration ttl = Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs());

        String oldAccessJti = null;
        long oldAccessRemainingMs = 0;
        if (oldAccessToken != null) {
            Claims oldAccessClaims = jwtTokenProvider.resolveAccessClaimsAllowExpired(oldAccessToken);
            if (oldAccessClaims != null && userId.equals(jwtTokenProvider.extractUserId(oldAccessClaims))) {
                oldAccessJti = jwtTokenProvider.extractJti(oldAccessClaims);
                oldAccessRemainingMs = jwtTokenProvider.getRemainingValidityMs(oldAccessClaims);
            }
        }

        boolean rotated = refreshTokenService.rotateAndBlacklist(
                userId, refreshToken, newRefreshToken, ttl, oldAccessJti, oldAccessRemainingMs);
        if (!rotated) {
            refreshTokenService.delete(userId);
            log.warn("event=refresh_token_reuse_suspected userId={}", userId);
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

    @Transactional
    public WithdrawResponse withdraw(Long userId, Claims accessClaims) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() == UserStatus.WITHDRAWN) {
            throw new BusinessException(UserErrorCode.ALREADY_WITHDRAWN);
        }

        user.withdraw();

        String jti = jwtTokenProvider.extractJti(accessClaims);
        long remainingMs = jwtTokenProvider.getRemainingValidityMs(accessClaims);
        LocalDateTime withdrawnAt = LocalDateTime.now();

        registerPostCommitTokenInvalidation(userId, jti, remainingMs);

        return new WithdrawResponse(withdrawnAt);
    }

    private void registerPostCommitTokenInvalidation(Long userId, String jti, long remainingMs) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            invalidateTokensWithRetry(userId, jti, remainingMs);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                invalidateTokensWithRetry(userId, jti, remainingMs);
            }
        });
    }

    private void invalidateTokensWithRetry(Long userId, String jti, long remainingMs) {
        for (int attempt = 1; attempt <= TOKEN_INVALIDATION_MAX_ATTEMPTS; attempt++) {
            try {
                tokenBlacklistService.blacklistWithdrawn(jti, remainingMs);
                refreshTokenService.delete(userId);
                return;
            } catch (Exception e) {
                if (attempt == TOKEN_INVALIDATION_MAX_ATTEMPTS) {
                    log.error("event=withdraw_token_invalidation_failed userId={} jti={} "
                            + "reason=MANUAL_REDIS_CLEANUP_REQUIRED", userId, jti, e);
                } else {
                    log.warn("event=withdraw_token_invalidation_retry attempt={} userId={} error={}",
                            attempt, userId, e.getMessage());
                    sleepBeforeRetry(attempt);
                }
            }
        }
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(TOKEN_INVALIDATION_RETRY_BACKOFF_MS * attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private TokenResponse issueTokens(Long userId) {
        String accessToken = jwtTokenProvider.createAccessToken(userId);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenService.save(
                userId, refreshToken, Duration.ofMillis(jwtTokenProvider.getRefreshTokenValidityMs()));

        return new TokenResponse(accessToken, refreshToken);
    }
}