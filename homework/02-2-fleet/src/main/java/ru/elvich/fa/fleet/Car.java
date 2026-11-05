package ru.elvich.fa.fleet;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "cars")
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 150)
    private String brand;
    @Column(nullable = false)
    private int manufactureYear;
    @Column(nullable = false)
    private LocalDate registeredOn;
    @Column(nullable = false, length = 250)
    private String ownerName;
    @Column(nullable = false)
    private long mileage;

    protected Car() {}

    Car(CarRequest request) {
        update(request);
    }

    void update(CarRequest request) {
        brand = request.brand().trim();
        manufactureYear = request.manufactureYear();
        registeredOn = request.registeredOn();
        ownerName = request.ownerName().trim();
        mileage = request.mileage();
    }

    public Long getId() { return id; }
    public String getBrand() { return brand; }
    public int getManufactureYear() { return manufactureYear; }
    public LocalDate getRegisteredOn() { return registeredOn; }
    public String getOwnerName() { return ownerName; }
    public long getMileage() { return mileage; }
}
