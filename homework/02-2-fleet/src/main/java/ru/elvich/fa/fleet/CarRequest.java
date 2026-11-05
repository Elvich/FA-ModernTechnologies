package ru.elvich.fa.fleet;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record CarRequest(
        @NotBlank(message = "Укажите марку") @Size(max = 150, message = "Марка: максимум 150 символов") String brand,
        @NotNull(message = "Укажите год выпуска") @Min(value = 1886, message = "Год выпуска должен быть не ранее 1886") Integer manufactureYear,
        @NotNull(message = "Укажите дату постановки на учёт") LocalDate registeredOn,
        @NotBlank(message = "Укажите ФИО владельца") @Size(max = 250, message = "ФИО: максимум 250 символов") String ownerName,
        @NotNull(message = "Укажите пробег") @PositiveOrZero(message = "Пробег не может быть отрицательным") Long mileage) {
}
