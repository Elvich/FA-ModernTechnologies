package ru.elvich.fa.fleet;

import java.time.Clock;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class FleetService {
    private final CarRepository repository;
    private final Clock clock;

    public FleetService(CarRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public List<Car> list(String query, String direction) {
        if (!direction.equals("asc") && !direction.equals("desc")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Порядок сортировки: asc или desc");
        }
        String search = query.strip().toLowerCase(Locale.ROOT);
        Comparator<Car> order = Comparator.comparingInt(Car::getManufactureYear);
        if (direction.equals("desc")) order = order.reversed();
        return repository.findAll().stream()
                .filter(car -> matches(car, search))
                .sorted(order.thenComparing(Car::getId)).toList();
    }

    private boolean matches(Car car, String search) {
        return search.isEmpty() || List.of(car.getId().toString(), car.getBrand(),
                Integer.toString(car.getManufactureYear()), car.getRegisteredOn().toString(),
                car.getOwnerName(), Long.toString(car.getMileage())).stream()
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(search));
    }

    public Car get(long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Машина не найдена"));
    }

    @Transactional
    public Car create(CarRequest request) {
        validateYear(request);
        return repository.save(new Car(request));
    }

    @Transactional
    public Car update(long id, CarRequest request) {
        validateYear(request);
        Car car = get(id);
        car.update(request);
        return repository.save(car);
    }

    @Transactional
    public void delete(long id) {
        repository.delete(get(id));
    }

    private void validateYear(CarRequest request) {
        if (request.manufactureYear() > LocalDate.now(clock).getYear() + 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Год выпуска не может превышать текущий год более чем на один");
        }
    }

    public List<DayCount> histogram() {
        LocalDate today = LocalDate.now(clock);
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (int offset = 6; offset >= 0; offset--) counts.put(today.minusDays(offset), 0L);
        for (Car car : repository.findAll()) {
            counts.computeIfPresent(car.getRegisteredOn(), (date, count) -> count + 1);
        }
        return counts.entrySet().stream().map(day -> new DayCount(day.getKey(), day.getValue())).toList();
    }

    public record DayCount(LocalDate date, long count) {}
}
