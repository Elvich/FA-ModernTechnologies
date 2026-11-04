package ru.elvich.fa.library;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.http.HttpStatus.*;

@RestController
@RequestMapping("/api")
public class BookController {
    private final BookRepository books;
    private final Clock clock;

    public BookController(BookRepository books, Clock clock) {
        this.books = books;
        this.clock = clock;
    }

    @GetMapping("/today")
    public LocalDate today() {
        return LocalDate.now(clock);
    }

    @GetMapping("/books")
    public List<Book> list(@RequestParam(defaultValue = "") String q,
                           @RequestParam(defaultValue = "asc") String direction) {
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ResponseStatusException(BAD_REQUEST, "Сортировка должна быть asc или desc");
        }
        String query = q.trim().toLowerCase(Locale.ROOT);
        Comparator<Book> comparator = Comparator.comparing((Book book) -> book.issuedOn);
        if (direction.equals("desc")) comparator = comparator.reversed();
        return books.findAll().stream()
                .filter(book -> matches(book, query))
                .sorted(comparator.thenComparing(book -> book.id))
                .toList();
    }

    private boolean matches(Book book, String query) {
        return (book.id + " " + book.title + " " + book.publisher + " " + book.issuedOn
                + " " + book.studentName + " " + Objects.toString(book.returnedOn, ""))
                .toLowerCase(Locale.ROOT).contains(query);
    }

    @GetMapping("/books/{id}")
    public Book get(@PathVariable long id) {
        return books.findById(id).orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Книга не найдена"));
    }

    @PostMapping("/books")
    public ResponseEntity<Book> create(@Valid @RequestBody BookInput input) {
        Book book = new Book();
        book.update(input);
        books.save(book);
        return ResponseEntity.created(URI.create("/api/books/" + book.id)).body(book);
    }

    @PutMapping("/books/{id}")
    public Book update(@PathVariable long id, @Valid @RequestBody BookInput input) {
        Book book = get(id);
        book.update(input);
        return books.save(book);
    }

    @DeleteMapping("/books/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        books.delete(get(id));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/books/histogram")
    public List<DayCount> histogram() {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int offset = 6; offset >= 0; offset--) counts.put(today.minusDays(offset), 0L);
        for (Book book : books.findAll()) {
            counts.computeIfPresent(book.issuedOn, (day, count) -> count + 1);
        }
        return counts.entrySet().stream().map(entry -> new DayCount(entry.getKey(), entry.getValue())).toList();
    }

    public record DayCount(LocalDate date, long count) {}
}
