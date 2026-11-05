package ru.elvich.fa.students;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class StudentsHttpTest {
    @Autowired Environment environment;
    @Autowired StudentRepository repository;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach void clear() { repository.deleteAll(); }

    HttpResponse<String> request(String method, String path, String json) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + environment.getProperty("local.server.port") + path))
                .header("Content-Type", "application/json");
        builder.method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test void crudPersistsAndRendersStudent(CapturedOutput output) throws Exception {
        var created = request("POST", "/db/students", "{\"id\":1,\"firstName\":\"Иван\",\"lastName\":\"Иванов\"}");
        assertThat(created.statusCode()).isEqualTo(201);
        assertThat(created.headers().firstValue("Location")).contains("/db/students/1");
        assertThat(request("GET", "/db/students", null).body()).contains("Иван", "Иванов", "\"id\":1");
        assertThat(request("GET", "/db/students/1", null).statusCode()).isEqualTo(200);
        assertThat(output).contains("Student with id=1 found");
        var page = request("GET", "/web/students", null);
        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("Иванов", "FirstName", "LastName").doesNotContain("th:each");
        assertThat(request("PUT", "/db/students/1", "{\"firstName\":\"Пётр\",\"lastName\":\"Петров\"}").statusCode()).isEqualTo(200);
        assertThat(repository.findById(1L).orElseThrow().getFirstName()).isEqualTo("Пётр");
        assertThat(request("DELETE", "/db/students/1", null).statusCode()).isEqualTo(204);
        assertThat(repository.count()).isZero();
        assertThat(request("GET", "/db/students", null).body()).isEqualTo("[]");
    }

    @Test void missingStudentReturns404AndLogsWarning(CapturedOutput output) throws Exception {
        assertThat(request("GET", "/db/students/99", null).statusCode()).isEqualTo(404);
        assertThat(request("PUT", "/db/students/99", "{\"firstName\":\"A\",\"lastName\":\"B\"}").statusCode()).isEqualTo(404);
        assertThat(request("DELETE", "/db/students/99", null).statusCode()).isEqualTo(404);
        assertThat(output).contains("WARN", "Student with id=99 not found");
        assertThat(repository.count()).isZero();
    }

    @Test void duplicateIdDoesNotOverwriteAndInvalidInputDoesNotWrite() throws Exception {
        String student = "{\"id\":7,\"firstName\":\"Анна\",\"lastName\":\"Смирнова\"}";
        assertThat(request("POST", "/db/students", student).statusCode()).isEqualTo(201);
        assertThat(request("POST", "/db/students", "{\"id\":7,\"firstName\":\"Other\",\"lastName\":\"Name\"}").statusCode()).isEqualTo(409);
        for (String invalid : new String[]{"{}", "{\"id\":0,\"firstName\":\"A\",\"lastName\":\"B\"}", "{\"id\":2,\"firstName\":\" \",\"lastName\":\"B\"}", "not-json"}) {
            assertThat(request("POST", "/db/students", invalid).statusCode()).isEqualTo(400);
        }
        assertThat(request("PUT", "/db/students/7", "{\"firstName\":\"\",\"lastName\":\"B\"}").statusCode()).isEqualTo(400);
        assertThat(repository.count()).isEqualTo(1);
        assertThat(repository.findById(7L).orElseThrow().getFirstName()).isEqualTo("Анна");
    }

    @Test void emptyPageAndEscapedNames() throws Exception {
        assertThat(request("GET", "/web/students", null).body()).contains("Студентов пока нет");
        assertThat(request("GET", "/", null).statusCode()).isEqualTo(302);
        assertThat(request("POST", "/db/students", "{\"id\":2,\"firstName\":\"<script>alert(1)</script>\",\"lastName\":\"Test\"}").statusCode()).isEqualTo(201);
        assertThat(request("GET", "/web/students", null).body()).contains("&lt;script&gt;").doesNotContain("<script>alert(1)</script>");
    }
}
