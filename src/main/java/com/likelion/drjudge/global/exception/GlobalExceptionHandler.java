package com.likelion.drjudge.global.exception;

import com.likelion.drjudge.domain.jwt.filter.TraceIdFilter;
import com.likelion.drjudge.global.response.ApiResponse;
import com.likelion.drjudge.global.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private String traceIdOf(HttpServletRequest request) {
        return (String) request.getAttribute(TraceIdFilter.TRACE_ID);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException exception, HttpServletRequest request) {

        ErrorCode errorCode = exception.getErrorCode();

        log.warn("event=exception_handled reason={}, code={}, message={}, traceId={}, details={}",
                errorCode, errorCode.getCode(), exception.getMessage(), traceIdOf(request), exception.getContext());

        ErrorResponse errorResponse = ErrorResponse.of(
                errorCode, errorCode.getMessage(), request.getRequestURI());

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            MethodArgumentNotValidException exception, HttpServletRequest request) {

        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .orElse(CommonErrorCode.INVALID_INPUT_VALUE.getMessage());

        log.warn("event=exception_handled reason={}, code={}, message={}, traceId={}",
                CommonErrorCode.INVALID_INPUT_VALUE, CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                message, traceIdOf(request));

        ErrorResponse errorResponse = ErrorResponse.of(
                CommonErrorCode.INVALID_INPUT_VALUE, message, request.getRequestURI());

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception, HttpServletRequest request) {

        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(ConstraintViolation::getMessage)
                .orElse(CommonErrorCode.INVALID_INPUT_VALUE.getMessage());

        ErrorResponse errorResponse = ErrorResponse.of(
                CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus().value(),
                CommonErrorCode.INVALID_INPUT_VALUE.getCode(), message, request.getRequestURI());

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequestException(
            Exception exception, HttpServletRequest request) {

        log.warn("event=exception_handled reason={}, code={}, message={}, traceId={}",
                CommonErrorCode.INVALID_INPUT_VALUE, CommonErrorCode.INVALID_INPUT_VALUE.getCode(),
                exception.getMessage(), traceIdOf(request));

        ErrorResponse errorResponse = ErrorResponse.of(
                CommonErrorCode.INVALID_INPUT_VALUE,
                CommonErrorCode.INVALID_INPUT_VALUE.getMessage(), request.getRequestURI());

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpServletRequest request) {

        String traceId = traceIdOf(request);

        if (exception.isForReturnValue()) {
            log.error("event=return_value_validation_failed traceId={}", traceId, exception);

            ErrorResponse errorResponse = ErrorResponse.of(
                    CommonErrorCode.INTERNAL_SERVER_ERROR,
                    CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(), request.getRequestURI());

            return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                    .body(ApiResponse.error(errorResponse));
        }

        log.warn("event=exception_handled reason={}, code={}, traceId={}",
                CommonErrorCode.INVALID_INPUT_VALUE, CommonErrorCode.INVALID_INPUT_VALUE.getCode(), traceId);

        ErrorResponse errorResponse = ErrorResponse.of(
                CommonErrorCode.INVALID_INPUT_VALUE,
                CommonErrorCode.INVALID_INPUT_VALUE.getMessage(), request.getRequestURI());

        return ResponseEntity.status(CommonErrorCode.INVALID_INPUT_VALUE.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception exception, HttpServletRequest request) {

        log.error("event=unhandled_exception traceId={}", traceIdOf(request), exception);

        ErrorResponse errorResponse = ErrorResponse.of(
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                CommonErrorCode.INTERNAL_SERVER_ERROR.getMessage(), request.getRequestURI());

        return ResponseEntity.status(CommonErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(ApiResponse.error(errorResponse));
    }
}