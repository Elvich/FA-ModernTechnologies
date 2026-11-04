package ru.elvich.fa.library;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.time.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(LibraryHttpTest.FixedClockConfig.class)
class LibraryHttpTest {
    @Autowired Environment environment;
    @Autowired BookRepository books;
    private final HttpClient client = HttpClient.newHttpClient();

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfig {
        @Bean @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-11-04T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @BeforeEach void clear() { books.deleteAll(); }

    HttpResponse<String> request(String method, String path, String json) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + environment.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json");
        builder.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    String json(String title, LocalDate issued, LocalDate returned) {
        return "{\"title\":\"" + title + "\",\"publisher\":\"Наука\",\"issuedOn\":\"" + issued
                + "\",\"studentName\":\"Тестовый студент\",\"returnedOn\":" + (returned == null ? "null" : "\"" + returned + "\"") + "}";
    }

    Book fixture(String title, LocalDate issued) {
        Book book = new Book();
        book.update(new BookInput(title, "Наука", issued, "Тестовый студент", null));
        return books.save(book);
    }

    @Test void createReadUpdateDeleteAndMissingRecord() throws Exception {
        LocalDate date = LocalDate.of(2026, 1, 5);
        var created = request("POST", "/api/books", json("Java", date, null));
        assertThat(created.statusCode()).isEqualTo(201);
        String location = created.headers().firstValue("Location").orElseThrow();
        assertThat(request("GET", location, null).body()).contains("Java", "Наука", "2026-01-05");
        var updated = request("PUT", location, json("Spring", date, date.plusDays(1)));
        assertThat(updated.statusCode()).isEqualTo(200);
        assertThat(updated.body()).contains("Spring", "2026-01-06");
        assertThat(books.findAll()).hasSize(1);
        assertThat(request("DELETE", location, null).statusCode()).isEqualTo(204);
        assertThat(request("GET", location, null).statusCode()).isEqualTo(404);
        assertThat(request("PUT", location, json("Java", date, null)).statusCode()).isEqualTo(404);
        assertThat(request("DELETE", location, null).statusCode()).isEqualTo(404);
        assertThat(books.count()).isZero();
    }

    @Test void searchAcrossFieldsAndSortBothWays() throws Exception {
        fixture("Поздняя", LocalDate.of(2026, 2, 10));
        fixture("Ранняя", LocalDate.of(2026, 1, 5));
        String asc = request("GET", "/api/books", null).body();
        assertThat(asc.indexOf("Ранняя")).isLessThan(asc.indexOf("Поздняя"));
        String desc = request("GET", "/api/books?direction=desc", null).body();
        assertThat(desc.indexOf("Поздняя")).isLessThan(desc.indexOf("Ранняя"));
        String title = request("GET", "/api/books?q=" + URLEncoder.encode("ранняя", StandardCharsets.UTF_8), null).body();
        assertThat(title).contains("Ранняя").doesNotContain("Поздняя");
        assertThat(request("GET", "/api/books?q=2026-02-10", null).body()).contains("Поздняя").doesNotContain("Ранняя");
        assertThat(request("GET", "/api/books?q=" + URLEncoder.encode("наука", StandardCharsets.UTF_8), null).body()).contains("Поздняя", "Ранняя");
        assertThat(request("GET", "/api/books?q=no-match", null).body()).isEqualTo("[]");
        assertThat(request("GET", "/api/books?direction=invalid", null).statusCode()).isEqualTo(400);
    }

    @Test void histogramHasSevenDaysZerosAndExcludesOutsideDates() throws Exception {
        LocalDate today = LocalDate.of(2026, 11, 4);
        fixture("До периода", today.minusDays(7));
        fixture("Начало периода", today.minusDays(6));
        fixture("Сегодня 1", today);
        fixture("Сегодня 2", today);
        fixture("Будущее", today.plusDays(1));
        String histogram = request("GET", "/api/books/histogram", null).body();
        assertThat(histogram).contains("\"date\":\"" + today.minusDays(6) + "\",\"count\":1");
        assertThat(histogram).contains("\"date\":\"" + today + "\",\"count\":2");
        assertThat(histogram).contains("\"date\":\"" + today.minusDays(3) + "\",\"count\":0");
        assertThat(histogram).doesNotContain(today.minusDays(7).toString(), today.plusDays(1).toString());
        assertThat(histogram.split("\"date\":", -1)).hasSize(8);
        assertThat(request("GET", "/api/today", null).body()).isEqualTo("\"" + today + "\"");
    }

    @Test void invalidInputDoesNotWriteAndUiIsAvailableWithoutLogin() throws Exception {
        LocalDate date = LocalDate.of(2026, 1, 5);
        assertThat(request("POST", "/api/books", json(" ", date, null)).statusCode()).isEqualTo(400);
        var invalidDates = request("POST", "/api/books", json("Java", date, date.minusDays(1)));
        assertThat(invalidDates.statusCode()).isEqualTo(400);
        assertThat(invalidDates.body()).contains("Дата сдачи");
        assertThat(request("POST", "/api/books", "{}").statusCode()).isEqualTo(400);
        assertThat(request("POST", "/api/books", "not-json").statusCode()).isEqualTo(400);
        assertThat(books.count()).isZero();
        var page = request("GET", "/", null);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Библиотека", "publisher", "chart");
    }
}
