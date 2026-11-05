package ru.elvich.fa.students;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StudentUpdate(
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {
}
