package com.bordeauxbrothers.parkshark.feature.division.api;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:division_e2e;"
                        + "MODE=PostgreSQL;"
                        + "DATABASE_TO_LOWER=TRUE;"
                        + "DEFAULT_NULL_ORDERING=HIGH;"
                        + "DB_CLOSE_DELAY=-1"
        }
)
@ActiveProfiles("test")
class DivisionEndToEndTest {

    private static final int DIRECTOR_ID = 900001;
    private static final String NAME = "ParkShark Antwerpen";
    private static final String ORIGINAL_NAME = "Antwerp Parking";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        cleanUp();

        jdbcTemplate.update("""
                INSERT INTO parkshark.person (
                    id,
                    first_name,
                    last_name,
                    email,
                    password_hash,
                    registration_date,
                    role
                )
                VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?)
                """,
                DIRECTOR_ID,
                "Ned",
                "Stark",
                "ned.division-test@example.com",
                "unused-test-password-hash",
                "MANAGER"
        );
    }

    @AfterEach
    void cleanUp() {
        // Eerst de division verwijderen vanwege de foreign key.
        jdbcTemplate.update(
                "DELETE FROM parkshark.division WHERE director_id = ?",
                DIRECTOR_ID
        );

        jdbcTemplate.update(
                "DELETE FROM parkshark.person WHERE id = ?",
                DIRECTOR_ID
        );
    }

    @Test
    void testDatabase_isH2() {
        jdbcTemplate.execute(
                (org.springframework.jdbc.core.ConnectionCallback<Void>) connection -> {
                    String database = connection.getMetaData()
                            .getDatabaseProductName();
                    String url = connection.getMetaData().getURL();

                    System.out.println("Test database: " + database);
                    System.out.println("Test JDBC URL: " + url);

                    assertThat(database).isEqualTo("H2");
                    assertThat(url).startsWith("jdbc:h2:mem:division_e2e");

                    return null;
                }
        );
    }

    @Test
    void createDivision_withValidRequest_returnsCreatedAndSavesDivision() {
        // GIVEN
        Map<String, Object> request =
                createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID);

        // WHEN + THEN: controleer de HTTP-response
        given()
                .baseUri("http://localhost")
                .port(port)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/division")
                .then()
                .log().ifValidationFails()
                .statusCode(201)
                .body("name", equalTo(NAME))
                .body("originalName", equalTo(ORIGINAL_NAME))
                .body("directorId", equalTo(DIRECTOR_ID));

        // THEN: controleer ook wat werkelijk opgeslagen is
        assertThat(countTestDivisions()).isEqualTo(1);

        Map<String, Object> savedDivision = jdbcTemplate.queryForMap("""
                SELECT name, original_name, director_id
                FROM parkshark.division
                WHERE director_id = ?
                """, DIRECTOR_ID);

        assertThat(savedDivision.get("name")).isEqualTo(NAME);
        assertThat(savedDivision.get("original_name"))
                .isEqualTo(ORIGINAL_NAME);
        assertThat(savedDivision.get("director_id"))
                .isEqualTo(DIRECTOR_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void createDivision_withInvalidName_returnsBadRequest(String name) {
        assertRejected(
                createRequest(name, ORIGINAL_NAME, DIRECTOR_ID)
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void createDivision_withInvalidOriginalName_returnsBadRequest(
            String originalName) {

        assertRejected(
                createRequest(NAME, originalName, DIRECTOR_ID)
        );
    }

    @Test
    void createDivision_withoutDirector_returnsBadRequest() {
        Map<String, Object> request =
                createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID);

        request.remove("directorId");

        assertRejected(request);
    }

    @Test
    void createDivision_withNullDirector_returnsBadRequest() {
        assertRejected(
                createRequest(NAME, ORIGINAL_NAME, null)
        );
    }

    private void assertRejected(Map<String, Object> request) {
        Long countBefore = countAllDivisions();

        given()
                .baseUri("http://localhost")
                .port(port)
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/division")
                .then()
                .log().ifValidationFails()
                .statusCode(400);

        assertThat(countAllDivisions()).isEqualTo(countBefore);
    }

    private Map<String, Object> createRequest(
            String name,
            String originalName,
            Integer directorId) {

        // HashMap laat null toe, zodat we die kunnen testen.
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("originalName", originalName);
        request.put("directorId", directorId);
        return request;
    }

    private Long countTestDivisions() {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM parkshark.division
                WHERE director_id = ?
                """, Long.class, DIRECTOR_ID);
    }

    private Long countAllDivisions() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parkshark.division",
                Long.class
        );
    }
}