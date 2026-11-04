package ru.elvich.fa.library;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, String>> invalid(MethodArgumentNotValidException error) {
        String message = error.getBindingResult().getAllErrors().stream()
                .map(item -> item.getDefaultMessage()).sorted().findFirst().orElse("Проверьте поля формы");
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> malformed(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "Некорректный формат данных или даты"));
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<Map<String, String>> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).body(Map.of("message", error.getReason() == null ? "Ошибка запроса" : error.getReason()));
    }
}
