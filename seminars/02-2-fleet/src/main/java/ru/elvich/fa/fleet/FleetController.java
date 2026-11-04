package ru.elvich.fa.fleet;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cars")
public class FleetController {
    private final FleetService service;

    public FleetController(FleetService service) { this.service = service; }

    @GetMapping
    public List<Car> list(@RequestParam(defaultValue = "") String q,
                         @RequestParam(defaultValue = "asc") String direction) {
        return service.list(q, direction);
    }

    @GetMapping("/{id}")
    public Car get(@PathVariable long id) { return service.get(id); }

    @GetMapping("/histogram")
    public List<FleetService.DayCount> histogram() { return service.histogram(); }

    @PostMapping
    public ResponseEntity<Car> create(@Valid @RequestBody CarRequest request) {
        Car car = service.create(request);
        return ResponseEntity.created(URI.create("/api/cars/" + car.getId())).body(car);
    }

    @PutMapping("/{id}")
    public Car update(@PathVariable long id, @Valid @RequestBody CarRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
