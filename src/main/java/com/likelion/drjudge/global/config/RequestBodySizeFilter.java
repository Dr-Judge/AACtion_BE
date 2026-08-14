package com.likelion.drjudge.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.likelion.drjudge.global.exception.CommonErrorCode;
import com.likelion.drjudge.global.response.ApiResponse;
import com.likelion.drjudge.global.response.ErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * application/json 요청 body 크기를 제한한다.
 * server.tomcat.max-http-form-post-size / max-swallow-size는 폼 파라미터 파싱과
 * 거부된 요청 폐기에만 적용되고 JSON @RequestBody에는 적용되지 않는다(CodeRabbit 지적, 확인됨).
 * 판정 요청의 imageBase64가 JSON body로 들어오기 때문에 별도 필터로 막는다.
 * 한계: Content-Length 헤더 기반이라, 헤더 없이 chunked로 보내는 요청은 걸러내지 못한다.
 */
@Component
@RequiredArgsConstructor
public class RequestBodySizeFilter extends OncePerRequestFilter {

    private static final long MAX_BODY_SIZE_BYTES = 10L * 1024 * 1024; // 10MB

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (request.getContentLengthLong() > MAX_BODY_SIZE_BYTES) {
            writeTooLarge(response, request.getRequestURI());
            return;
        }
        chain.doFilter(request, response);
    }

    private void writeTooLarge(HttpServletResponse response, String path) throws IOException {
        response.setStatus(HttpStatus.PAYLOAD_TOO_LARGE.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ErrorResponse errorResponse = ErrorResponse.of(
                CommonErrorCode.PAYLOAD_TOO_LARGE,
                "요청 본문이 너무 큽니다 (최대 10MB).",
                path);

        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(errorResponse)));
    }
}
