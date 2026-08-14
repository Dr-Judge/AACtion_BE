package com.likelion.drjudge.domain.jwt.jwt;

import com.likelion.drjudge.global.response.ApiResponse;
import tools.jackson.databind.ObjectMapper;
import com.likelion.drjudge.domain.auth.exception.AuthErrorCode;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import com.likelion.drjudge.global.exception.ErrorCode;
import com.likelion.drjudge.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = Boolean.TRUE.equals(request.getAttribute("ALREADY_LOGGED_OUT"))
                ? AuthErrorCode.ALREADY_LOGGED_OUT
                : CommonErrorCode.UNAUTHORIZED;

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode, errorCode.getMessage(), request.getRequestURI());

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorResponse));
    }
}