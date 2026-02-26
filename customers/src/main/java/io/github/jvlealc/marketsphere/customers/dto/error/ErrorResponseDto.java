package io.github.jvlealc.marketsphere.customers.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String message,
        List<ValidationErrorDto> errors,
        String path
) {
    public ErrorResponseDto(int status, String message, List<ValidationErrorDto> errors, String path) {
        this(Instant.now(), status, message, errors, path);
    }

    public static ErrorResponseDto badRequest(String message, String path) {
        return new ErrorResponseDto(Instant.now(), HttpStatus.BAD_REQUEST.value(), message, List.of(), path);
    }

    public static ErrorResponseDto conflict(String message, String path) {
        return new ErrorResponseDto(Instant.now(), HttpStatus.CONFLICT.value(), message, List.of(), path);
    }

    public static ErrorResponseDto notFound(String message, String path) {
        return new ErrorResponseDto(Instant.now(), HttpStatus.NOT_FOUND.value(), message, List.of(), path);
    }
}
