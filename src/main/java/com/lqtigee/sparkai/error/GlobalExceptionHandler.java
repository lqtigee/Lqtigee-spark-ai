package com.lqtigee.sparkai.error;

import com.lqtigee.sparkai.dto.ApiErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorDto> handleApiException(ApiException exception, HttpServletRequest request) {
        ApiErrorDto body = new ApiErrorDto(
                exception.code(),
                exception.getMessage(),
                exception.detail(),
                Instant.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(exception.status()).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        ApiErrorDto body = new ApiErrorDto(
                ErrorCode.VALIDATION_FAILED,
                "Request validation failed",
                exception.getBindingResult().getFieldErrors().isEmpty()
                        ? null
                        : exception.getBindingResult().getFieldErrors().getFirst().getField(),
                Instant.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleGenericException(Exception exception, HttpServletRequest request) {
        ApiErrorDto body = new ApiErrorDto(
                ErrorCode.INTERNAL_ERROR,
                "Internal server error",
                null,
                Instant.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
