package com.raghunath.smartstore.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtExpired(ExpiredJwtException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "JWT token has expired",
                "Please refresh your token or login again. Token expired at: " + ex.getClaims().getExpiration(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(io.jsonwebtoken.JwtException.class)
    public ResponseEntity<ErrorResponse> handleJwtInvalid(JwtException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid JWT token",
                "Token validation failed: " + ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid argument provided",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex, WebRequest request) {
        ErrorResponse err = new ErrorResponse(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(err);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex, WebRequest request) {
        ErrorResponse err = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        ErrorResponse err = new ErrorResponse(
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(err);
    }

    // 409 Duplicate key (Mongo unique index violation)
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Map<String,Object>> handleDuplicateKey(DuplicateKeyException ex) {
        Map<String,Object> body = Map.of("success", false, "message", "Resource conflict - duplicate key", "detail", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String,Object>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        Map<String,Object> body = Map.of(
                "success", false,
                "message", "Validation failed",
                "errors", ex.getConstraintViolations()
        );
        return ResponseEntity.badRequest().body(body);
    }

    // 400 Malformed JSON
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedJson(HttpMessageNotReadableException ex, WebRequest request) {
        ErrorResponse err = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Malformed JSON request",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.badRequest().body(err);
    }

    // 400 Method argument type mismatch
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String msg = String.format("Parameter '%s' should be of type %s", ex.getName(), ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        ErrorResponse err = new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Invalid parameter", msg, request.getDescription(false));
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        ErrorResponse err = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Missing request parameter",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.badRequest().body(err);
    }

    // 429 Too many requests
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ErrorResponse> handleTooManyRequests(TooManyRequestsException ex, WebRequest request) {
        ErrorResponse err = new ErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too many requests",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(err);
    }

    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<Map<String,Object>> handleConflict(ResourceConflictException ex) {
        Map<String,Object> body = Map.of("success", false, "message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Map<String,Object>> handleBadRequest(BadRequestException ex) {
        Map<String,Object> body = Map.of("success", false, "message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String,String> errors = new HashMap<>();
        for (FieldError err : ex.getBindingResult().getFieldErrors()) {
            errors.put(err.getField(), err.getDefaultMessage());
        }
        Map<String,Object> body = Map.of(
                "success", false,
                "message", "Validation failed",
                "errors", errors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException ex, WebRequest request) {
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Runtime error occurred",
                ex.getMessage(),
                request.getDescription(false)
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
