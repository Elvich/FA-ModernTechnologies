package ru.elvich.fa.library;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "books")
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 200)
    public String title;
    @Column(nullable = false, length = 200)
    public String publisher;
    @Column(nullable = false)
    public LocalDate issuedOn;
    @Column(nullable = false, length = 200)
    public String studentName;
    public LocalDate returnedOn;

    public Book() {}

    void update(BookInput input) {
        title = input.title().trim();
        publisher = input.publisher().trim();
        issuedOn = input.issuedOn();
        studentName = input.studentName().trim();
        returnedOn = input.returnedOn();
    }
}
