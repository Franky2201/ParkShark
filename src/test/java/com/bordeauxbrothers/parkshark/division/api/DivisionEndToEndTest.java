package com.bordeauxbrothers.parkshark.division.api;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashMap;
import java.util.List;
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
    private static final int SECOND_DIRECTOR_ID = 900002;

    private static final String NAME = "ParkShark Antwerpen";
    private static final String ORIGINAL_NAME = "Antwerp Parking";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        assertTestDatabase();
        deleteTestData();

        // Deze tests verwachten een lege division-tabel bij de start.
        assertThat(countAllDivisions())
                .as("Geen vooraf geladen divisions in de testdatabase")
                .isZero();

        insertDirector(DIRECTOR_ID, "Ned", "Stark");
        insertDirector(SECOND_DIRECTOR_ID, "Tyrion", "Lannister");
    }

    @AfterEach
    void cleanUp() {
        assertTestDatabase();
        deleteTestData();
    }

    @Nested
    class CreateDivision {

        @Test
        void withValidRequest_returnsCreatedAndSavesDivision() {
            Map<String, Object> request =
                    createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID);

            api()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/division")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("name", equalTo(NAME))
                    .body("originalName", equalTo(ORIGINAL_NAME))
                    .body("directorId", equalTo(DIRECTOR_ID));

            assertThat(countAllDivisions()).isEqualTo(1L);

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
        void withInvalidName_returnsBadRequest(String name) {
            assertRejected(
                    createRequest(name, ORIGINAL_NAME, DIRECTOR_ID)
            );
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void withInvalidOriginalName_returnsBadRequest(
                String originalName) {

            assertRejected(
                    createRequest(NAME, originalName, DIRECTOR_ID)
            );
        }

        @Test
        void withoutDirector_returnsBadRequest() {
            Map<String, Object> request =
                    createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID);

            request.remove("directorId");

            assertRejected(request);
        }

        @Test
        void withNullDirector_returnsBadRequest() {
            assertRejected(
                    createRequest(NAME, ORIGINAL_NAME, null)
            );
        }
    }

    @Nested
    class GetAllDivisions {

        @Test
        void withoutDivisions_returnsOkAndEmptyList() {
            List<Map<String, Object>> result = getAllDivisions();

            assertThat(result).isEmpty();
        }

        @Test
        void withMultipleDivisions_returnsAllDivisionsWithCorrectData() {
            // GIVEN: verschillende directors en een gedeelde director.
            insertDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID);
            insertDivision(
                    "ParkShark Gent",
                    "Ghent Parking",
                    SECOND_DIRECTOR_ID
            );
            insertDivision(
                    "ParkShark Leuven",
                    "Leuven Parking",
                    DIRECTOR_ID
            );

            // WHEN
            List<Map<String, Object>> result = getAllDivisions();

            // THEN: controleer volledige records, ongeacht hun volgorde.
            assertThat(result).containsExactlyInAnyOrder(
                    createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID),
                    createRequest(
                            "ParkShark Gent",
                            "Ghent Parking",
                            SECOND_DIRECTOR_ID
                    ),
                    createRequest(
                            "ParkShark Leuven",
                            "Leuven Parking",
                            DIRECTOR_ID
                    )
            );
        }

        @Test
        void afterCreatingDivision_returnsCreatedDivision() {
            // GIVEN: aanmaken via de echte POST-route.
            Map<String, Object> request =
                    createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID);

            api()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/division")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            // WHEN
            List<Map<String, Object>> result = getAllDivisions();

            // THEN
            assertThat(result).containsExactly(request);
        }
    }

    private RequestSpecification api() {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .accept(ContentType.JSON);
    }

    private List<Map<String, Object>> getAllDivisions() {
        return api()
                .when()
                .get("/division")
                .then()
                .log().ifValidationFails()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .extract()
                .jsonPath()
                .getList("$");
    }

    private void assertRejected(Map<String, Object> request) {
        Long countBefore = countAllDivisions();

        api()
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

        // HashMap ondersteunt null voor de validatietests.
        Map<String, Object> request = new HashMap<>();
        request.put("name", name);
        request.put("originalName", originalName);
        request.put("directorId", directorId);
        return request;
    }

    private void insertDirector(
            int id,
            String firstName,
            String lastName) {

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
                id,
                firstName,
                lastName,
                "director-" + id + "@division-test.example",
                "unused-test-password-hash",
                "MANAGER"
        );
    }

    private void insertDivision(
            String name,
            String originalName,
            int directorId) {

        jdbcTemplate.update("""
                INSERT INTO parkshark.division (
                    name,
                    original_name,
                    director_id
                )
                VALUES (?, ?, ?)
                """,
                name,
                originalName,
                directorId
        );
    }

    private void deleteTestData() {
        jdbcTemplate.update("""
                DELETE FROM parkshark.division
                WHERE director_id IN (?, ?)
                """,
                DIRECTOR_ID,
                SECOND_DIRECTOR_ID
        );

        jdbcTemplate.update("""
                DELETE FROM parkshark.person
                WHERE id IN (?, ?)
                """,
                DIRECTOR_ID,
                SECOND_DIRECTOR_ID
        );
    }

    private Long countAllDivisions() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parkshark.division",
                Long.class
        );
    }

    private void assertTestDatabase() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            assertThat(connection.getMetaData().getDatabaseProductName())
                    .isEqualTo("H2");

            assertThat(connection.getMetaData().getURL())
                    .startsWith("jdbc:h2:mem:division_e2e");

            return null;
        });
    }
}