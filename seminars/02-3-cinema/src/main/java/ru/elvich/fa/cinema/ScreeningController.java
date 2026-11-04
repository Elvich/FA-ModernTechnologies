package ru.elvich.fa.cinema;

import jakarta.validation.Valid;
import java.net.URI;
import java.time.*;
import java.util.*;
import java.util.stream.IntStream;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/screenings")
public class ScreeningController {
    private final ScreeningRepository repository;
    private final Clock clock;

    public ScreeningController(ScreeningRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @GetMapping
    public List<Screening> list(@RequestParam(defaultValue = "") String q,
                               @RequestParam(defaultValue = "asc") String direction) {
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "direction: допустимы asc или desc");
        }
        var sort = Sort.by(Sort.Direction.fromString(direction), "startsAt").and(Sort.by("id"));
        String query = q.strip().toLowerCase(Locale.ROOT);
        return repository.findAll(sort).stream().filter(s -> searchable(s).contains(query)).toList();
    }

    private String searchable(Screening s) {
        return String.join(" ", s.getId().toString(), s.getFilmTitle(), s.getStudio(),
                s.getStartsAt().toString(), s.getTicketCapacity().toString(), s.getSoldTickets().toString())
                .toLowerCase(Locale.ROOT);
    }

    @GetMapping("/{id}")
    public Screening get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Сеанс не найден"));
    }

    @PostMapping
    public ResponseEntity<Screening> create(@Valid @RequestBody ScreeningInput input) {
        Screening saved = repository.save(copy(input, new Screening()));
        return ResponseEntity.created(URI.create("/api/screenings/" + saved.getId())).body(saved);
    }

    @PutMapping("/{id}")
    public Screening update(@PathVariable Long id, @Valid @RequestBody ScreeningInput input) {
        return repository.save(copy(input, get(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        repository.delete(get(id));
    }

    private Screening copy(ScreeningInput input, Screening entity) {
        entity.setFilmTitle(input.filmTitle().strip());
        entity.setStudio(input.studio().strip());
        entity.setStartsAt(input.startsAt());
        entity.setTicketCapacity(input.ticketCapacity());
        entity.setSoldTickets(input.soldTickets());
        return entity;
    }

    public record DayCount(LocalDate date, long count) { }

    @GetMapping("/histogram")
    public List<DayCount> histogram() {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Long> counts = new HashMap<>();
        for (Screening s : repository.findAll()) {
            counts.merge(s.getStartsAt().toLocalDate(), 1L, Long::sum);
        }
        return IntStream.rangeClosed(0, 6).mapToObj(i -> today.minusDays(6 - i))
                .map(date -> new DayCount(date, counts.getOrDefault(date, 0L))).toList();
    }
}
