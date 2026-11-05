package ru.elvich.fa.warehouse;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {
    private final EquipmentRepository repository;
    private final Clock clock;
    public EquipmentController(EquipmentRepository repository, Clock clock) {
        this.repository = repository; this.clock = clock;
    }
    @GetMapping
    public List<Equipment> list(@RequestParam(defaultValue = "") String q,
                                @RequestParam(defaultValue = "asc") String order) {
        if (!Set.of("asc", "desc").contains(order))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Порядок должен быть asc или desc");
        String needle = q.strip().toLowerCase(Locale.ROOT);
        Comparator<Equipment> comparator = Comparator.comparing(Equipment::getImportedOn);
        if (order.equals("desc")) comparator = comparator.reversed();
        return repository.findAll().stream().filter(e -> Stream.of(e.getId(), e.getKind(),
                e.getEquipmentGroup(), e.getImportedOn(), e.getExportedOn(), e.getDriverName())
                .filter(Objects::nonNull).anyMatch(v -> v.toString().toLowerCase(Locale.ROOT).contains(needle)))
                .sorted(comparator.thenComparing(Equipment::getId)).toList();
    }
    @GetMapping("/{id}")
    public Equipment get(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Техника не найдена"));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Equipment create(@Valid @RequestBody Equipment input) {
        input.setId(null); return repository.save(input);
    }
    @PutMapping("/{id}")
    public Equipment update(@PathVariable Long id, @Valid @RequestBody Equipment input) {
        get(id); input.setId(id); return repository.save(input);
    }
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { repository.delete(get(id)); }
    public record DailyCount(LocalDate date, long count) {}
    @GetMapping("/histogram")
    public List<DailyCount> histogram() {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Long> days = new LinkedHashMap<>();
        for (int n = 6; n >= 0; n--) days.put(today.minusDays(n), 0L);
        for (Equipment e : repository.findAll()) {
            if (days.containsKey(e.getExportedOn())) days.compute(e.getExportedOn(), (date, n) -> n + 1);
        }
        return days.entrySet().stream().map(e -> new DailyCount(e.getKey(), e.getValue())).toList();
    }
}
