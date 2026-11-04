package ru.elvich.fa.warehouse;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Entity
public class Equipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200)
    private String kind;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200)
    private String equipmentGroup;
    @NotNull @Column(nullable = false)
    private LocalDate importedOn;
    private LocalDate exportedOn;
    @NotBlank @Size(max = 200) @Column(nullable = false, length = 200)
    private String driverName;

    @AssertTrue(message = "Дата вывоза не может быть раньше даты ввоза")
    public boolean isDateOrderValid() {
        return importedOn == null || exportedOn == null || !exportedOn.isBefore(importedOn);
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getEquipmentGroup() { return equipmentGroup; }
    public void setEquipmentGroup(String equipmentGroup) { this.equipmentGroup = equipmentGroup; }
    public LocalDate getImportedOn() { return importedOn; }
    public void setImportedOn(LocalDate importedOn) { this.importedOn = importedOn; }
    public LocalDate getExportedOn() { return exportedOn; }
    public void setExportedOn(LocalDate exportedOn) { this.exportedOn = exportedOn; }
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
}
