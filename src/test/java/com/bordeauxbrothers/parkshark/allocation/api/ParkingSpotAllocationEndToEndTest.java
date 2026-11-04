package com.bordeauxbrothers.parkshark.allocation.api;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * PSA1 functional contract tests. These intentionally expose bugs in version 3.0.
 * Keep the current request shape: licencePlate: {number, country}, parkingLotId.
 * Success is 200 as in the current controller; change SUCCESS to 201 if the
 * controller is changed to @ResponseStatus(HttpStatus.CREATED).
 * No security matrix: every request uses one valid member account.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:psa1_e2e;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password="
        })
@ActiveProfiles("test")
@Import(ParkingSpotAllocationEndToEndTest.FixedTime.class)
@Execution(ExecutionMode.SAME_THREAD)
class ParkingSpotAllocationEndToEndTest {
    private static final int SUCCESS = 200;
    // Deliberately different IDs: using memberId to look up a lot must fail.
    private static final int MEMBER = 930001;
    private static final int MANAGER = 930002;
    private static final int LOT = 930101;
    private static final int OTHER_LOT = 930102;
    private static final int DIVISION = 930201;
    private static final int POSTAL = 930301;
    private static final String EMAIL = "member@psa1-test.example";
    private static final String PASSWORD = "TestPassword123!";
    private static final String PLATE = "PSA1-001";
    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwordEncoder;

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedTime {
        @Bean @Primary
        Clock psa1Clock() { return Clock.fixed(NOW, ZoneOffset.UTC); }
    }

    @BeforeEach
    void setUp() {
        // Never delete data unless this is our dedicated in-memory database.
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(connection.getMetaData().getURL()).isEqualTo("jdbc:h2:mem:psa1_e2e");
            return null;
        });
        jdbc.update("DELETE FROM parkshark.parking_spot_allocation WHERE parking_lot_id IN (?, ?)", LOT, OTHER_LOT);
        jdbc.update("DELETE FROM parkshark.parking_lot WHERE id IN (?, ?)", LOT, OTHER_LOT);
        jdbc.update("DELETE FROM parkshark.division WHERE id = ?", DIVISION);
        jdbc.update("DELETE FROM parkshark.person WHERE id IN (?, ?)", MEMBER, MANAGER);
        jdbc.update("DELETE FROM parkshark.license_plate WHERE id = ?", MEMBER);
        jdbc.update("DELETE FROM parkshark.address WHERE id IN (?, ?, ?)", MEMBER, LOT, OTHER_LOT);
        jdbc.update("DELETE FROM parkshark.postal_code WHERE id = ?", POSTAL);

        jdbc.update("INSERT INTO parkshark.postal_code (id, zip_code, label) VALUES (?, '9000', 'PSA1 City')", POSTAL);
        for (int id : new int[]{MEMBER, LOT, OTHER_LOT}) {
            jdbc.update("INSERT INTO parkshark.address (id, street, number, postal_code_id) VALUES (?, 'Test Street', ?, ?)",
                    id, String.valueOf(id), POSTAL);
        }
        jdbc.update("INSERT INTO parkshark.license_plate (id, number, country) VALUES (?, ?, 'BE')", MEMBER, PLATE);
        jdbc.update("""
                INSERT INTO parkshark.person
                (id, first_name, last_name, email, password_hash, registration_date, role,
                 address_id, phone_number, license_plate_id, membership)
                VALUES (?, 'Test', 'Member', ?, ?, CURRENT_DATE, 'USER', ?, '+32470000111', ?, 'BRONZE')
                """, MEMBER, EMAIL, passwordEncoder.encode(PASSWORD), MEMBER, MEMBER);
        jdbc.update("""
                INSERT INTO parkshark.person
                (id, first_name, last_name, email, password_hash, registration_date, role)
                VALUES (?, 'Test', 'Manager', 'manager@psa1-test.example', ?, CURRENT_DATE, 'MANAGER')
                """, MANAGER, passwordEncoder.encode(PASSWORD));
        jdbc.update("""
                INSERT INTO parkshark.division (id, name, original_name, director_id)
                VALUES (?, 'PSA1 Division', 'PSA1 Original', ?)
                """, DIVISION, MANAGER);
        for (int id : new int[]{LOT, OTHER_LOT}) {
            jdbc.update("""
                    INSERT INTO parkshark.parking_lot
                    (id, name, category, max_capacity, price_per_hour_in_euro, address_id, division_id, contact_person_id)
                    VALUES (?, ?, 'UNDERGROUND', 2, 2.50, ?, ?, ?)
                    """, id, "PSA1 Lot " + id, id, DIVISION, MANAGER);
        }
    }

    @Nested
    class SuccessfulAllocation {
        @ParameterizedTest
        @ValueSource(strings = {"BRONZE", "SILVER", "GOLD"})
        void matchingPlate_createsActiveAllocationAndPersistsResponse(String membership) {
            membership(membership);
            int id = post(request(PLATE, LOT)).statusCode(SUCCESS)
                    .body("id", greaterThan(0))
                    .body("memberId", equalTo(MEMBER))
                    .body("parkingLotId", equalTo(LOT))
                    .body("licensePlateNumber", equalTo(PLATE))
                    .body("status", equalTo("ACTIVE"))
                    .body("endTime", nullValue())
                    .extract().path("id");
            assertThat(count()).isEqualTo(1L);
            Map<String, Object> row = jdbc.queryForMap(
                    "SELECT * FROM parkshark.parking_spot_allocation WHERE id = ?", id);
            assertThat(row.get("member_id")).isEqualTo(MEMBER);
            assertThat(row.get("parking_lot_id")).isEqualTo(LOT);
            assertThat(row.get("license_plate_number")).isEqualTo(PLATE);
            assertThat(row.get("status")).isEqualTo("ACTIVE");
            assertThat(row.get("end_time")).isNull();
            assertThat(Objects.requireNonNull(jdbc.queryForObject(
                    "SELECT start_time FROM parkshark.parking_spot_allocation WHERE id = ?",
                    OffsetDateTime.class, id)).toInstant()).isEqualTo(NOW);
        }

        @Test
        void startTimeComesFromInjectedClock() {
            String start = post(request(PLATE, LOT)).statusCode(SUCCESS)
                    .extract().path("startTime");
            assertThat(OffsetDateTime.parse(start).toInstant()).isEqualTo(NOW);
        }

        @Test
        void goldCanParkAnotherCar_andActualPlateIsStored() {
            membership("GOLD");
            int id = post(request("RENTAL-123", LOT)).statusCode(SUCCESS)
                    .body("licensePlateNumber", equalTo("RENTAL-123"))
                    .extract().path("id");
            assertThat(jdbc.queryForObject(
                    "SELECT license_plate_number FROM parkshark.parking_spot_allocation WHERE id = ?",
                    String.class, id)).isEqualTo("RENTAL-123");
        }

        @Test
        void generatedIdsAreUnique() {
            int first = post(request(PLATE, LOT)).statusCode(SUCCESS).extract().path("id");
            // End first session before starting another; no assumption about simultaneous sessions.
            jdbc.update("UPDATE parkshark.parking_spot_allocation SET status = 'PASSIVE', end_time = start_time WHERE id = ?", first);
            int second = post(request(PLATE, LOT)).statusCode(SUCCESS).extract().path("id");
            assertThat(second).isNotEqualTo(first);
            assertThat(count()).isEqualTo(2L);
        }
    }

    @Nested
    class BusinessRules {
        @ParameterizedTest
        @ValueSource(strings = {"BRONZE", "SILVER"})
        void differentPlateIsRejected(String membership) {
            membership(membership);
            post(request("OTHER-123", LOT)).statusCode(400);
            assertThat(count()).isZero();
        }

        @Test
        void unknownLotIsRejected() {
            post(request(PLATE, 999999)).statusCode(404);
            assertThat(count()).isZero();
        }

        @Test
        void exactlyFullLotIsRejected() {
            seedAllocation(LOT, "ACTIVE");
            seedAllocation(LOT, "ACTIVE");
            post(request(PLATE, LOT)).statusCode(409);
            assertThat(count()).isEqualTo(2L);
        }

        @Test
        void lastAvailableSpotCanBeTaken_butNextRequestIsRejected() {
            seedAllocation(LOT, "ACTIVE");
            post(request(PLATE, LOT)).statusCode(SUCCESS);
            assertThat(count()).isEqualTo(2L);
            post(request(PLATE, LOT)).statusCode(409);
            assertThat(count()).isEqualTo(2L);
        }

        @Test
        void allocationsInOtherLotsDoNotConsumeRequestedLotCapacity() {
            seedAllocation(OTHER_LOT, "ACTIVE");
            seedAllocation(OTHER_LOT, "ACTIVE");
            post(request(PLATE, LOT)).statusCode(SUCCESS).body("parkingLotId", equalTo(LOT));
            assertThat(count()).isEqualTo(3L);
        }

        @Test
        void stoppedAndInvoicedAllocationsDoNotConsumeCapacity() {
            seedAllocation(LOT, "PASSIVE");
            seedAllocation(LOT, "INVOICED");
            post(request(PLATE, LOT)).statusCode(SUCCESS);
            assertThat(count()).isEqualTo(3L);
        }
    }

    @Nested
    class RequestValidation {
        @Test
        void missingPlateIsRejected() {
            Map<String, Object> body = request(PLATE, LOT);
            body.remove("licencePlate");
            post(body).statusCode(400);
            assertThat(count()).isZero();
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        void blankPlateIsRejectedEvenForGold(String plate) {
            membership("GOLD");
            post(request(plate, LOT)).statusCode(400);
            assertThat(count()).isZero();
        }

        @Test
        void missingPlateNumberIsRejectedEvenForGold() {
            membership("GOLD");
            Map<String, Object> body = request(PLATE, LOT);
            body.put("licencePlate", Map.of("country", "BE"));
            post(body).statusCode(400);
            assertThat(count()).isZero();
        }

        @Test
        void missingParkingLotIdIsRejected() {
            Map<String, Object> body = request(PLATE, LOT);
            body.remove("parkingLotId");
            post(body).statusCode(400);
            assertThat(count()).isZero();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, -1})
        void nonPositiveParkingLotIdIsRejected(int lotId) {
            post(request(PLATE, lotId)).statusCode(400);
            assertThat(count()).isZero();
        }
    }

    private Map<String, Object> request(String plate, int lot) {
        Map<String, Object> body = new HashMap<>();
        body.put("licencePlate", Map.of("number", plate, "country", "BE"));
        body.put("parkingLotId", lot);
        return body;
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return given().baseUri("http://localhost").port(port)
                .auth().preemptive().basic(EMAIL, PASSWORD)
                .contentType(ContentType.JSON).accept(ContentType.JSON)
                .body(body).when().post("/allocation")
                .then().log().ifValidationFails();
    }

    private void membership(String value) {
        jdbc.update("UPDATE parkshark.person SET membership = ? WHERE id = ?", value, MEMBER);
    }

    private long count() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM parkshark.parking_spot_allocation WHERE parking_lot_id IN (?, ?)",
                Long.class, LOT, OTHER_LOT);
    }

    private void seedAllocation(int lot, String status) {
        OffsetDateTime start = OffsetDateTime.ofInstant(NOW.minusSeconds(7200), ZoneOffset.UTC);
        OffsetDateTime end = status.equals("ACTIVE") ? null : start.plusHours(1);
        jdbc.update("""
                INSERT INTO parkshark.parking_spot_allocation
                (member_id, parking_lot_id, license_plate_number, start_time, end_time, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """, MEMBER, lot, PLATE, start, end, status);
    }
}
