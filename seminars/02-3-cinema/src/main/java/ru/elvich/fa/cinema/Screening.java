package ru.elvich.fa.cinema;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "screenings")
public class Screening {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 200)
    private String filmTitle;
    @Column(nullable = false, length = 200)
    private String studio;
    @Column(nullable = false)
    private LocalDateTime startsAt;
    @Column(nullable = false)
    private Integer ticketCapacity;
    @Column(nullable = false)
    private Integer soldTickets;

    public Long getId() { return id; }
    public String getFilmTitle() { return filmTitle; }
    public void setFilmTitle(String value) { filmTitle = value; }
    public String getStudio() { return studio; }
    public void setStudio(String value) { studio = value; }
    public LocalDateTime getStartsAt() { return startsAt; }
    public void setStartsAt(LocalDateTime value) { startsAt = value; }
    public Integer getTicketCapacity() { return ticketCapacity; }
    public void setTicketCapacity(Integer value) { ticketCapacity = value; }
    public Integer getSoldTickets() { return soldTickets; }
    public void setSoldTickets(Integer value) { soldTickets = value; }
}
