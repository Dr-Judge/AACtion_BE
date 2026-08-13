package com.likelion.drjudge.domain.jwt.jwt;

import tools.jackson.databind.ObjectMapper;
import com.likelion.drjudge.domain.jwt.filter.TraceIdFilter;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import com.likelion.drjudge.global.exception.ErrorCode;
import com.likelion.drjudge.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ErrorCode errorCode = CommonErrorCode.FORBIDDEN;

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode,
                errorCode.getMessage(),
                request.getRequestURI(),
                (String) request.getAttribute(TraceIdFilter.TRACE_ID)
        );

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}