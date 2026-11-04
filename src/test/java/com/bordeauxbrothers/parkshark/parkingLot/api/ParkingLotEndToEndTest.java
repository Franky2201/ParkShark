package com.bordeauxbrothers.parkshark.parkingLot.api;

import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:parking_lot_e2e;"
                        + "MODE=PostgreSQL;"
                        + "DATABASE_TO_LOWER=TRUE;"
                        + "DEFAULT_NULL_ORDERING=HIGH;"
                        + "DB_CLOSE_DELAY=-1"
        }
)
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("PL1 - ParkingLot - End-to-End Tests")
class ParkingLotEndToEndTest {

    private static final int MANAGER_ID = 900101;
    private static final int DIVISION_ID = 900101;
    private static final int MEMBER_ID = 900102;
    private static final int MEMBER_ADDRESS_ID = 900102;
    private static final int MEMBER_POSTAL_CODE_ID = 900102;
    private static final int MEMBER_PLATE_ID = 900102;

    private static final String MANAGER_EMAIL = "manager@parking-lot-test.example";
    private static final String MEMBER_EMAIL = "member@parking-lot-test.example";
    private static final String TEST_PASSWORD = "Heisenberg123!";

    private static final String NAME = "Central Station Underground";
    private static final String STREET = "CanterSteen";
    private static final String NUMBER = "12";
    private static final String ZIP_CODE = "1000";
    private static final String LABEL = "Brussels";

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        assertTestDatabase();
        deleteTestData();

        insertManager();
        insertDivision();
        insertMember();
    }

    @AfterEach
    void cleanUp() {
        assertTestDatabase();
        deleteTestData();
    }

    @Nested
    @DisplayName("POST /parking-lots - Happy path")
    class HappyPath {

        @Test
        @DisplayName("Should create the parking lot and return 201 Created when valid data is provided")
        void createParkingLot_withValidRequest_returnsCreatedAndSavesParkingLot() {
            // Given
            Map<String, Object> request = validRequest();

            // When & Then
            api()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("id", notNullValue())
                    .body("name", equalTo(NAME))
                    .body("category", equalTo("UNDERGROUND"))
                    .body("maxCapacity", equalTo(250))
                    .body("contactPersonId", equalTo(MEMBER_ID))
                    .body("divisionId", equalTo(DIVISION_ID))
                    .body("address.street", equalTo(STREET))
                    .body("address.number", equalTo(NUMBER))
                    .body("address.postal_code.zip_code", equalTo(ZIP_CODE))
                    .body("address.postal_code.label", equalTo(LABEL));

            Map<String, Object> saved = jdbcTemplate.queryForMap("""
                    SELECT pl.name, pl.category, pl.max_capacity, pl.price_per_hour_in_euro,
                           pl.contact_person_id, pl.division_id,
                           a.street, a.number, pc.zip_code, pc.label
                    FROM parkshark.parking_lot pl
                    JOIN parkshark.address a ON a.id = pl.address_id
                    JOIN parkshark.postal_code pc ON pc.id = a.postal_code_id
                    WHERE pl.name = ?
                    """, NAME);

            assertThat(saved.get("category")).isEqualTo("UNDERGROUND");
            assertThat(saved.get("max_capacity")).isEqualTo(250);
            assertThat((BigDecimal) saved.get("price_per_hour_in_euro")).isEqualByComparingTo("3.50");
            assertThat(saved.get("contact_person_id")).isEqualTo(MEMBER_ID);
            assertThat(saved.get("division_id")).isEqualTo(DIVISION_ID);
            assertThat(saved.get("street")).isEqualTo(STREET);
            assertThat(saved.get("number")).isEqualTo(NUMBER);
            assertThat(saved.get("zip_code")).isEqualTo(ZIP_CODE);
            assertThat(saved.get("label")).isEqualTo(LABEL);
        }
    }

    @Nested
    @DisplayName("POST /parking-lots - Validation and exceptions")
    class ValidationAndExceptions {

        static Stream<Arguments> invalidFields() {
            return Stream.of(
                    Arguments.of("name", null),
                    Arguments.of("name", ""),
                    Arguments.of("name", "   "),
                    Arguments.of("category", null),
                    Arguments.of("category", "STREET"),
                    Arguments.of("maxCapacity", null),
                    Arguments.of("maxCapacity", 0),
                    Arguments.of("maxCapacity", -1),
                    Arguments.of("pricePerHour", null),
                    Arguments.of("pricePerHour", new BigDecimal("0")),
                    Arguments.of("pricePerHour", new BigDecimal("-1.00")),
                    Arguments.of("pricePerHour", new BigDecimal("3.555")),
                    Arguments.of("contactPersonId", null),
                    Arguments.of("divisionId", null)
            );
        }

        @ParameterizedTest(name = "{0} = {1}")
        @MethodSource("invalidFields")
        @DisplayName("Should return 400 Bad Request and save nothing when a field is invalid")
        void createParkingLot_withInvalidField_returnsBadRequestAndSavesNothing(String field, Object value) {
            // Given
            Map<String, Object> request = validRequest();
            request.put(field, value);

            // When & Then
            api()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

            assertThat(countParkingLots()).isZero();
        }

        @Test
        @DisplayName("Should return 400 Bad Request and save nothing when the address is missing")
        void createParkingLot_withoutAddress_returnsBadRequestAndSavesNothing() {
            // Given
            Map<String, Object> request = validRequest();
            request.remove("address");

            // When & Then
            api()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(400);

            assertThat(countParkingLots()).isZero();
        }

        @Test
        @DisplayName("Should return 404 Not Found and save nothing when the division does not exist")
        void createParkingLot_withUnknownDivision_returnsNotFoundAndSavesNothing() {
            // Given
            Map<String, Object> request = validRequest();
            request.put("divisionId", 987654);

            // When & Then
            api()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(404);

            assertThat(countParkingLots()).isZero();
        }

        @Test
        @DisplayName("Should return 409 Conflict and save nothing extra when the name is already taken")
        @SuppressWarnings("unchecked")
        void createParkingLot_withExistingName_returnsConflictAndSavesNothing() {
            // Given: a first parking lot created through the real POST route
            api()
                    .contentType(ContentType.JSON)
                    .body(validRequest())
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201);

            Map<String, Object> sameName = validRequest();
            ((Map<String, Object>) sameName.get("address")).put("street", "Other street");

            // When & Then
            api()
                    .contentType(ContentType.JSON)
                    .body(sameName)
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(409);

            assertThat(countParkingLots()).isEqualTo(1L);
        }
    }

    @Nested
    @DisplayName("POST /parking-lots - Security and authorization")
    class SecurityAndAuthorization {

        @Test
        @DisplayName("Should return 401 Unauthorized and save nothing when no credentials are provided")
        void createParkingLot_withoutCredentials_returnsUnauthorizedAndSavesNothing() {
            // Given
            Map<String, Object> request = validRequest();

            // When & Then
            apiWithoutAuthentication()
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(401);

            assertThat(countParkingLots()).isZero();
        }

        @Test
        @DisplayName("Should return 403 Forbidden and save nothing when a member tries to create a parking lot")
        void createParkingLot_asMember_returnsForbiddenAndSavesNothing() {
            // Given
            Map<String, Object> request = validRequest();

            // When & Then
            apiAs(MEMBER_EMAIL, TEST_PASSWORD)
                    .contentType(ContentType.JSON)
                    .body(request)
                    .when()
                    .post("/parking-lots")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(403);

            assertThat(countParkingLots()).isZero();
        }
    }

    private Map<String, Object> validRequest() {
        // HashMap allows null values for the validation tests.
        Map<String, Object> postalCode = new HashMap<>();
        postalCode.put("zip_code", ZIP_CODE);
        postalCode.put("label", LABEL);

        Map<String, Object> address = new HashMap<>();
        address.put("street", STREET);
        address.put("number", NUMBER);
        address.put("postal_code", postalCode);

        Map<String, Object> request = new HashMap<>();
        request.put("name", NAME);
        request.put("category", "UNDERGROUND");
        request.put("maxCapacity", 250);
        request.put("pricePerHour", new BigDecimal("3.50"));
        request.put("address", address);
        request.put("contactPersonId", MEMBER_ID);
        request.put("divisionId", DIVISION_ID);
        return request;
    }

    private Long countParkingLots() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM parkshark.parking_lot WHERE division_id = ?", Long.class, DIVISION_ID);
    }

    private RequestSpecification api() {
        return apiAs(MANAGER_EMAIL, TEST_PASSWORD);
    }

    private RequestSpecification apiAs(String email, String password) {
        return apiWithoutAuthentication().auth().preemptive().basic(email, password);
    }

    private RequestSpecification apiWithoutAuthentication() {
        return given()
                .baseUri("http://localhost")
                .port(port)
                .accept(ContentType.JSON);
    }

    private void insertManager() {
        jdbcTemplate.update("""
                INSERT INTO parkshark.person (
                    id, first_name, last_name, email, password_hash, registration_date, role
                ) VALUES (?, 'Saul', 'Goodman', ?, ?, CURRENT_DATE, 'MANAGER')
                """, MANAGER_ID, MANAGER_EMAIL, passwordEncoder.encode(TEST_PASSWORD));
    }

    private void insertDivision() {
        jdbcTemplate.update("""
                INSERT INTO parkshark.division (id, name, original_name, director_id)
                VALUES (?, 'ParkShark Brussels', 'Brussels Parking', ?)
                """, DIVISION_ID, MANAGER_ID);
    }

    private void insertMember() {
        // USER is the member role in the migrations.
        jdbcTemplate.update("""
                INSERT INTO parkshark.postal_code (id, zip_code, label)
                VALUES (?, '3000', 'Parking Lot E2E City')
                """, MEMBER_POSTAL_CODE_ID);
        jdbcTemplate.update("""
                INSERT INTO parkshark.address (id, street, number, postal_code_id)
                VALUES (?, 'Teststraat', '1', ?)
                """, MEMBER_ADDRESS_ID, MEMBER_POSTAL_CODE_ID);
        jdbcTemplate.update("""
                INSERT INTO parkshark.license_plate (id, number, country)
                VALUES (?, 'E2E-PL-001', 'BE')
                """, MEMBER_PLATE_ID);
        jdbcTemplate.update("""
                INSERT INTO parkshark.person (
                    id, first_name, last_name, email, password_hash,
                    registration_date, role, address_id, phone_number,
                    license_plate_id, membership
                ) VALUES (?, 'Walter', 'White', ?, ?, CURRENT_DATE,
                          'USER', ?, '+32475000666', ?, 'BRONZE')
                """, MEMBER_ID, MEMBER_EMAIL, passwordEncoder.encode(TEST_PASSWORD),
                MEMBER_ADDRESS_ID, MEMBER_PLATE_ID);
    }

    private void deleteTestData() {
        // Parking lots first: they reference the address, contact person and division.
        List<Integer> parkingLotAddressIds = jdbcTemplate.queryForList(
                "SELECT address_id FROM parkshark.parking_lot WHERE division_id = ?",
                Integer.class, DIVISION_ID);
        jdbcTemplate.update("DELETE FROM parkshark.parking_lot WHERE division_id = ?", DIVISION_ID);
        parkingLotAddressIds.forEach(id ->
                jdbcTemplate.update("DELETE FROM parkshark.address WHERE id = ?", id));
        jdbcTemplate.update("""
                DELETE FROM parkshark.postal_code
                WHERE zip_code = ? AND label = ?
                  AND id NOT IN (SELECT postal_code_id FROM parkshark.address)
                """, ZIP_CODE, LABEL);

        jdbcTemplate.update("DELETE FROM parkshark.division WHERE id = ?", DIVISION_ID);
        jdbcTemplate.update("DELETE FROM parkshark.person WHERE id IN (?, ?)", MANAGER_ID, MEMBER_ID);
        jdbcTemplate.update("DELETE FROM parkshark.address WHERE id = ?", MEMBER_ADDRESS_ID);
        jdbcTemplate.update("DELETE FROM parkshark.license_plate WHERE id = ?", MEMBER_PLATE_ID);
        jdbcTemplate.update("DELETE FROM parkshark.postal_code WHERE id = ?", MEMBER_POSTAL_CODE_ID);
    }

    private void assertTestDatabase() {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:parking_lot_e2e");
            return null;
        });
    }
}
