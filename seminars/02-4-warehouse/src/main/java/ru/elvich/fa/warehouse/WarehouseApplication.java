package ru.elvich.fa.warehouse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.time.Clock;

@SpringBootApplication
public class WarehouseApplication {
    public static void main(String[] args) { SpringApplication.run(WarehouseApplication.class, args); }
    @Bean Clock clock() { return Clock.systemDefaultZone(); }
}
