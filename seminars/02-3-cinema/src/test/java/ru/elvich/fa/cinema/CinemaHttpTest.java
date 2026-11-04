package ru.elvich.fa.cinema;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.regex.Pattern;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(CinemaHttpTest.FixedClock.class)
class CinemaHttpTest {
    @Value("${local.server.port}") int port;
    @Autowired ScreeningRepository repository;
    private final HttpClient client = HttpClient.newHttpClient();

    @TestConfiguration
    static class FixedClock {
        @Bean @Primary Clock testClock() {
            return Clock.fixed(Instant.parse("2026-11-04T09:00:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    HttpResponse<String> call(String method, String path, String body) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10));
        if (body != null) request.header("Content-Type", "application/json");
        request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    String json(String title, String studio, String date, int total, int sold) {
        return """
                {"filmTitle":"%s","studio":"%s","startsAt":"%s","ticketCapacity":%d,"soldTickets":%d}
                """.formatted(title, studio, date, total, sold);
    }

    @Test
    void crudAndMissingRecord() throws Exception {
        var created = call("POST", "/api/screenings", json("Фильм", "Студия", "2026-11-04T18:30:00", 100, 12));
        assertThat(created.statusCode()).isEqualTo(201);
        String location = created.headers().firstValue("Location").orElseThrow();
        assertThat(call("GET", location, null).body()).contains("Фильм", "Студия", "\"soldTickets\":12");
        assertThat(call("PUT", location, json("Новый фильм", "Другая студия", "2026-11-05T12:00:00", 50, 50)).statusCode()).isEqualTo(200);
        assertThat(call("GET", location, null).body()).contains("Новый фильм", "\"soldTickets\":50");
        assertThat(repository.count()).isEqualTo(1);
        assertThat(call("DELETE", location, null).statusCode()).isEqualTo(204);
        assertThat(repository.count()).isZero();
        assertThat(call("GET", location, null).statusCode()).isEqualTo(404);
        assertThat(call("PUT", location, json("Нет", "Нет", "2026-11-05T12:00:00", 1, 0)).statusCode()).isEqualTo(404);
        assertThat(call("DELETE", location, null).statusCode()).isEqualTo(404);
    }

    @Test
    void searchAndSortByDateAscendingAndDescending() throws Exception {
        var late = call("POST", "/api/screenings", json("Поздний", "Орион", "2026-11-04T20:00:00", 123, 9));
        call("POST", "/api/screenings", json("Ранний", "Вега", "2026-11-02T09:00:00", 100, 0));
        String asc = call("GET", "/api/screenings", null).body();
        assertThat(asc).contains("Ранний", "Поздний");
        assertThat(asc.indexOf("Ранний")).isLessThan(asc.indexOf("Поздний"));
        String desc = call("GET", "/api/screenings?direction=desc", null).body();
        assertThat(desc).contains("Ранний", "Поздний");
        assertThat(desc.indexOf("Поздний")).isLessThan(desc.indexOf("Ранний"));
        for (String query : new String[]{"ОРИОН", "поздний", "2026-11-04", "123"}) {
            String result = call("GET", "/api/screenings?q=" + URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8), null).body();
            assertThat(result).contains("Поздний").doesNotContain("Ранний");
        }
        assertThat(call("GET", "/api/screenings?q=not-found", null).body()).isEqualTo("[]");
        assertThat(call("GET", "/api/screenings?direction=invalid", null).statusCode()).isEqualTo(400);
        String id = late.headers().firstValue("Location").orElseThrow().replace("/api/screenings/", "");
        assertThat(call("GET", "/api/screenings?q=" + id, null).body()).contains("Поздний");
    }

    @Test
    void histogramHasSevenDaysIncludingZerosAndExcludesOutsideWindow() throws Exception {
        for (String date : new String[]{"2026-10-28T23:59:59", "2026-10-29T00:00:00", "2026-11-04T00:00:00", "2026-11-04T23:59:59", "2026-11-05T00:00:00"}) {
            assertThat(call("POST", "/api/screenings", json("Фильм", "Студия", date, 10, 0)).statusCode()).isEqualTo(201);
        }
        String result = call("GET", "/api/screenings/histogram", null).body();
        assertThat(result).contains("{\"date\":\"2026-10-29\",\"count\":1}",
                "{\"date\":\"2026-10-30\",\"count\":0}", "{\"date\":\"2026-11-04\",\"count\":2}")
                .doesNotContain("2026-10-28", "2026-11-05");
        assertThat(Pattern.compile("\\\"date\\\"").matcher(result).results().count()).isEqualTo(7);
    }

    @Test
    void invalidInputsDoNotChangeDatabase() throws Exception {
        for (String input : new String[]{json(" ", "Студия", "2026-11-04T12:00:00", 10, 0),
                json("Фильм", "Студия", "2026-11-04T12:00:00", 10, 11),
                json("Фильм", "Студия", "2026-11-04T12:00:00", -1, 0),
                json("Фильм", "Студия", "2026-11-04T12:00:00", 10, -1),
                "{\"filmTitle\":\"Фильм\"}", "{", json("Фильм", "Студия", "wrong-date", 10, 0)}) {
            var response = call("POST", "/api/screenings", input);
            assertThat(response.statusCode()).isEqualTo(400);
            assertThat(response.body()).contains("message");
        }
        assertThat(repository.count()).isZero();
        var zero = call("POST", "/api/screenings", json("Фильм", "Студия", "2026-11-04T12:00:00", 0, 0));
        assertThat(zero.statusCode()).isEqualTo(201);
        String location = zero.headers().firstValue("Location").orElseThrow();
        assertThat(call("PUT", location, json("Ошибка", "Студия", "2026-11-04T12:00:00", 1, 2)).statusCode()).isEqualTo(400);
        assertThat(call("GET", location, null).body()).contains("Фильм").doesNotContain("Ошибка");
    }
}
