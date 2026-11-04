package ru.elvich.fa.warehouse;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class WarehouseIntegrationTest {
    @LocalServerPort int port;
    @Autowired EquipmentRepository repository;
    @Autowired ObjectMapper json;
    HttpClient client = HttpClient.newHttpClient();
    @BeforeEach void clear() { repository.deleteAll(); }
    HttpResponse<String> call(String method, String path, String body) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
    String data(String kind, String in, String out) {
        return "{\"kind\":\"" + kind + "\",\"equipmentGroup\":\"Компьютеры\",\"importedOn\":\"" + in
            + "\",\"exportedOn\":" + (out == null ? "null" : "\"" + out + "\"") + ",\"driverName\":\"Тестовый водитель\"}";
    }
    long create(String kind, String in, String out) throws Exception {
        var response = call("POST", "/api/equipment", data(kind, in, out));
        assertThat(response.statusCode()).isEqualTo(201);
        return json.readTree(response.body()).get("id").asLong();
    }
    @Test void crudSearchSortAndMissingRecords() throws Exception {
        long newer = create("Ноутбук", "2026-02-02", null);
        long older = create("Монитор", "2026-01-01", "2026-02-03");
        var rows = json.readTree(call("GET", "/api/equipment", null).body());
        assertThat(rows.get(0).get("id").asLong()).isEqualTo(older);
        assertThat(json.readTree(call("GET", "/api/equipment?order=desc", null).body()).get(0).get("id").asLong()).isEqualTo(newer);
        var found = json.readTree(call("GET", "/api/equipment?q=2026-02-03", null).body());
        assertThat(found.size()).isEqualTo(1);
        assertThat(found.get(0).get("kind").asText()).isEqualTo("Монитор");
        assertThat(call("PUT", "/api/equipment/" + newer, data("Принтер", "2026-02-02", null)).statusCode()).isEqualTo(200);
        assertThat(call("GET", "/api/equipment/" + newer, null).body()).contains("Принтер");
        assertThat(call("DELETE", "/api/equipment/" + newer, null).statusCode()).isEqualTo(204);
        assertThat(call("GET", "/api/equipment/" + newer, null).statusCode()).isEqualTo(404);
        assertThat(call("PUT", "/api/equipment/" + newer, data("Принтер", "2026-02-02", null)).statusCode()).isEqualTo(404);
        assertThat(call("DELETE", "/api/equipment/" + newer, null).statusCode()).isEqualTo(404);
    }
    @Test void histogramCountsExportDatesAndIncludesZeroDays() throws Exception {
        LocalDate today = LocalDate.now();
        String start = today.minusDays(20).toString();
        create("Сегодня", start, today.toString());
        create("Сегодня2", start, today.toString());
        create("Граница", start, today.minusDays(6).toString());
        create("Раньше", start, today.minusDays(7).toString());
        create("Будущее", start, today.plusDays(1).toString());
        create("Не вывезено", today.toString(), null);
        JsonNode days = json.readTree(call("GET", "/api/equipment/histogram", null).body());
        assertThat(days.size()).isEqualTo(7);
        assertThat(days.get(0).get("date").asText()).isEqualTo(today.minusDays(6).toString());
        assertThat(days.get(0).get("count").asLong()).isEqualTo(1);
        assertThat(days.get(1).get("count").asLong()).isZero();
        assertThat(days.get(6).get("count").asLong()).isEqualTo(2);
    }
    @Test void validationRejectsInvalidDatesBlankKindAndMalformedRequests() throws Exception {
        assertThat(call("POST", "/api/equipment", data("Техника", "2026-02-02", "2026-01-01")).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/api/equipment", data(" ", "2026-02-02", null)).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/api/equipment", "{}").statusCode()).isEqualTo(400);
        assertThat(call("GET", "/api/equipment?order=unknown", null).statusCode()).isEqualTo(400);
        assertThat(call("POST", "/api/equipment", "invalid").statusCode()).isEqualTo(400);
        assertThat(repository.count()).isZero();
    }
    @Test void browserPageIsAvailableWithoutLogin() throws Exception {
        var page = call("GET", "/", null);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Склад магазина техники");
    }
}
