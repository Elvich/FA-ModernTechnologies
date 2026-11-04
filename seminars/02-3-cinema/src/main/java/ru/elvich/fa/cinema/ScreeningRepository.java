package ru.elvich.fa.cinema;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
}
