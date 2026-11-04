package ru.elvich.fa.warehouse;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getAllErrors().stream()
            .map(error -> error.getDefaultMessage()).collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> malformed(Exception ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Проверьте формат чисел и дат"));
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException ex) {
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", reason(ex)));
    }
    private String reason(ResponseStatusException ex) { return ex.getReason() == null ? "Ошибка запроса" : ex.getReason(); }
}
