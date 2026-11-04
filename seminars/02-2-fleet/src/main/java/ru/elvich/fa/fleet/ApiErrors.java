package ru.elvich.fa.fleet;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage()).distinct().sorted().collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> unreadable(Exception exception) {
        return ResponseEntity.badRequest().body(Map.of("message", "Проверьте формат чисел, ID и даты (ГГГГ-ММ-ДД)"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> status(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(Map.of("message",
                exception.getReason() == null ? "Ошибка запроса" : exception.getReason()));
    }
}
