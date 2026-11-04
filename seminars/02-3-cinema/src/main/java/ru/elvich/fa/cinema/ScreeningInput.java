package ru.elvich.fa.cinema;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record ScreeningInput(
        @NotBlank(message = "Укажите название фильма") @Size(max = 200) String filmTitle,
        @NotBlank(message = "Укажите киностудию") @Size(max = 200) String studio,
        @NotNull(message = "Укажите дату и время сеанса") LocalDateTime startsAt,
        @NotNull(message = "Укажите число билетов") @Min(value = 0, message = "Число билетов не может быть отрицательным") Integer ticketCapacity,
        @NotNull(message = "Укажите число проданных билетов") @Min(value = 0, message = "Число проданных билетов не может быть отрицательным") Integer soldTickets) {
    @AssertTrue(message = "Проданных билетов не может быть больше, чем мест на сеанс")
    public boolean isTicketCountValid() {
        return ticketCapacity == null || soldTickets == null || soldTickets <= ticketCapacity;
    }
}
