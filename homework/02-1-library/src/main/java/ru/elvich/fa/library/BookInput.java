package ru.elvich.fa.library;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record BookInput(
        @NotBlank(message = "Укажите название") @Size(max = 200) String title,
        @NotBlank(message = "Укажите издательство") @Size(max = 200) String publisher,
        @NotNull(message = "Укажите дату выдачи") LocalDate issuedOn,
        @NotBlank(message = "Укажите ФИО студента") @Size(max = 200) String studentName,
        LocalDate returnedOn) {

    @AssertTrue(message = "Дата сдачи не может быть раньше даты выдачи")
    public boolean isReturnDateValid() {
        return returnedOn == null || issuedOn == null || !returnedOn.isBefore(issuedOn);
    }
}
