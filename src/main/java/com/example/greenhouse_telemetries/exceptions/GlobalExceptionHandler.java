package com.example.greenhouse_telemetries.exceptions;

import com.example.greenhouse_telemetries.DTO.error.ErrorResponseDTO;
import com.example.greenhouse_telemetries.exceptions.auth.UserAlreadyExistException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(UserAlreadyExistException exc) {
        log.warn("Signup conflict: {}", exc.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(HttpStatus.CONFLICT.value(), exc.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(MethodArgumentNotValidException exc) {
        log.warn("Validation error: {}", exc.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), exc.getMessage()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(BadCredentialsException exc) {
        log.warn("Authentication failed: {}", exc.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponseDTO(HttpStatus.UNAUTHORIZED.value(), exc.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(DataIntegrityViolationException exc) {
        log.error("Data integrity violation: ", exc);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponseDTO(HttpStatus.CONFLICT.value(), "Object already exists (check unique fields)"));
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(EntityNotFoundException exc) {
        log.warn("Entity with id not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(HttpStatus.NOT_FOUND.value(), exc.getMessage()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(BadRequestException exc) {
        log.warn("Bad request: {}", exc.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), exc.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(AccessDeniedException exc) {
        log.warn("Security alert - Access denied: {}", exc.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponseDTO(HttpStatus.FORBIDDEN.value(), exc.getMessage()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> exceptionHandle(HttpMessageNotReadableException exc) {
        log.warn("Request missing");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(HttpStatus.BAD_REQUEST.value(), "Request body is missing"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGlobal(Exception exc) {
        log.error("Internal server error: ", exc);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error"));
    }
}
