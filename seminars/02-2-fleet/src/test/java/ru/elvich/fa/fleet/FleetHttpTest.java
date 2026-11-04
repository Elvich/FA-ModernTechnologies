package ru.elvich.fa.fleet;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(FleetHttpTest.FixedClock.class)
class FleetHttpTest {
    @Autowired CarRepository repository;
    @Autowired Environment environment;
    private final HttpClient client = HttpClient.newHttpClient();
    private final JsonMapper json = JsonMapper.builder().build();

    @TestConfiguration
    static class FixedClock {
        @Bean @Primary Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void clear() { repository.deleteAll(); }

    private HttpResponse<String> request(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:"
                + environment.getProperty("local.server.port") + path));
        request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String data(String brand, int year, String date, long mileage) {
        return """
                {"brand":"%s","manufactureYear":%d,"registeredOn":"%s","ownerName":"Иванов Иван","mileage":%d}
                """.formatted(brand, year, date, mileage);
    }

    private long create(String brand, int year, String date) throws Exception {
        HttpResponse<String> response = request("POST", "/api/cars", data(brand, year, date, 12000));
        assertThat(response.statusCode()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asLong();
    }

    @Test
    void createReadUpdateAndDelete() throws Exception {
        long id = create("Toyota", 2020, "2026-11-01");
        HttpResponse<String> get = request("GET", "/api/cars/" + id, null);
        assertThat(get.statusCode()).isEqualTo(200);
        assertThat(json.readTree(get.body()).get("brand").asText()).isEqualTo("Toyota");
        HttpResponse<String> updated = request("PUT", "/api/cars/" + id, data("Honda", 2021, "2026-11-02", 18000));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(repository.findById(id).orElseThrow().getBrand()).isEqualTo("Honda");
        assertThat(request("DELETE", "/api/cars/" + id, null).statusCode()).isEqualTo(204);
        assertThat(repository.count()).isZero();
        assertThat(request("GET", "/api/cars/" + id, null).statusCode()).isEqualTo(404);
        assertThat(request("DELETE", "/api/cars/" + id, null).statusCode()).isEqualTo(404);
        assertThat(request("PUT", "/api/cars/" + id, data("Honda", 2021, "2026-11-02", 1)).statusCode()).isEqualTo(404);
    }

    @Test
    void searchAcrossFieldsAndSortByYear() throws Exception {
        long newer = create("Toyota", 2021, "2026-11-03");
        long older = create("Lada", 2000, "2026-11-01");
        JsonNode ascending = json.readTree(request("GET", "/api/cars", null).body());
        assertThat(ascending.size()).isEqualTo(2);
        assertThat(ascending.get(0).get("id").asLong()).isEqualTo(older);
        JsonNode descending = json.readTree(request("GET", "/api/cars?direction=desc", null).body());
        assertThat(descending.get(0).get("id").asLong()).isEqualTo(newer);
        for (String query : new String[]{"tOyOtA", "2021", "2026-11-03"}) {
            JsonNode result = json.readTree(request("GET", "/api/cars?q=" + query, null).body());
            assertThat(result.size()).isEqualTo(1);
            assertThat(result.get(0).get("id").asLong()).isEqualTo(newer);
        }
        String owner = URLEncoder.encode("ИВАНОВ", StandardCharsets.UTF_8);
        assertThat(json.readTree(request("GET", "/api/cars?q=" + owner, null).body()).size()).isEqualTo(2);
        assertThat(json.readTree(request("GET", "/api/cars?q=missing", null).body()).size()).isZero();
        assertThat(request("GET", "/api/cars?direction=wrong", null).statusCode()).isEqualTo(400);
    }

    @Test
    void histogramHasSevenDaysIncludingZerosAndExcludesOutsideDates() throws Exception {
        create("Before", 2000, "2026-10-28");
        create("First", 2000, "2026-10-29");
        create("Today1", 2000, "2026-11-04");
        create("Today2", 2000, "2026-11-04");
        create("Future", 2000, "2026-11-05");
        JsonNode days = json.readTree(request("GET", "/api/cars/histogram", null).body());
        assertThat(days.size()).isEqualTo(7);
        assertThat(days.get(0).get("date").asText()).isEqualTo("2026-10-29");
        assertThat(days.get(0).get("count").asLong()).isEqualTo(1);
        for (int index = 1; index < 6; index++) assertThat(days.get(index).get("count").asLong()).isZero();
        assertThat(days.get(6).get("date").asText()).isEqualTo("2026-11-04");
        assertThat(days.get(6).get("count").asLong()).isEqualTo(2);
    }

    @Test
    void invalidInputDoesNotChangeDatabase() throws Exception {
        assertThat(request("POST", "/api/cars", data("  ", 2020, "2026-11-01", 0)).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/cars", data("Lada", 2020, "2026-11-01", -1)).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/cars", data("Lada", 1800, "2026-11-01", 0)).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/cars", data("Lada", 2028, "2026-11-01", 0)).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/cars", data("Lada", 2020, "2026-02-30", 0)).statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/cars", "{}").statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/cars", "not json").statusCode()).isEqualTo(400);
        assertThat(request("GET", "/api/cars/not-a-number", null).statusCode()).isEqualTo(400);
        assertThat(repository.count()).isZero();
        assertThat(request("GET", "/api/cars/999999", null).body()).contains("Машина не найдена");
    }

    @Test
    void servesWebPageWithoutAuthentication() throws Exception {
        HttpResponse<String> page = request("GET", "/", null);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Автопарк", "car-form", "histogram");
        assertThat(request("GET", "/app.js", null).statusCode()).isEqualTo(200);
    }
}
