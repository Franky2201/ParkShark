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
import org.junit.jupiter.api.AfterEach;
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
import java.util.List;
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
    private static final int SUCCESS = 201;
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

    @Nested
    class StopAllocation {

        private static final int OTHER_MEMBER = 930003;
        private static final String OTHER_EMAIL = "other@psa2-test.example";

        @BeforeEach
        void createOtherMember() {
            // A second real member is needed to test allocation ownership.
            jdbc.update("""
                INSERT INTO parkshark.license_plate (id, number, country)
                VALUES (?, 'PSA2-OTHER', 'BE')
                """, OTHER_MEMBER);

            // Sharing an address is allowed; each member has their own licence plate.
            jdbc.update("""
                INSERT INTO parkshark.person (
                    id, first_name, last_name, email, password_hash,
                    registration_date, role, address_id, phone_number,
                    license_plate_id, membership
                )
                VALUES (
                    ?, 'Other', 'Member', ?, ?,
                    CURRENT_DATE, 'USER', ?, '+32470000222', ?, 'BRONZE'
                )
                """,
                    OTHER_MEMBER,
                    OTHER_EMAIL,
                    passwordEncoder.encode(PASSWORD),
                    MEMBER,
                    OTHER_MEMBER);
        }

        @AfterEach
        void removeOtherMember() {
            jdbc.update(
                    "DELETE FROM parkshark.person WHERE id = ?",
                    OTHER_MEMBER
            );
            jdbc.update(
                    "DELETE FROM parkshark.license_plate WHERE id = ?",
                    OTHER_MEMBER
            );
        }

        @Nested
        class SuccessfulStop {

            @ParameterizedTest
            @ValueSource(strings = {"BRONZE", "SILVER", "GOLD"})
            void ownerCanStopActiveAllocation(String membership) {
                membership(membership);
                int id = createAllocation("ACTIVE");
                OffsetDateTime originalStart = timeColumn(id, "start_time");

                var response = stop(id, EMAIL)
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", equalTo(id))
                        .body("memberId", equalTo(MEMBER))
                        .body("parkingLotId", equalTo(LOT))
                        .body("licensePlateNumber", equalTo(PLATE))
                        .body("status", equalTo("PASSIVE"))
                        .body("endTime", notNullValue())
                        .extract();

                assertThat(
                        OffsetDateTime.parse(response.path("startTime")).toInstant()
                ).isEqualTo(originalStart.toInstant());

                assertThat(
                        OffsetDateTime.parse(response.path("endTime")).toInstant()
                ).isEqualTo(NOW);

                // Check committed database state, not only the response.
                assertThat(status(id)).isEqualTo("PASSIVE");
                assertThat(timeColumn(id, "end_time").toInstant()).isEqualTo(NOW);
                assertThat(timeColumn(id, "start_time")).isEqualTo(originalStart);
                assertThat(count()).isEqualTo(1L);
            }

            @Test
            void stoppingOneAllocationDoesNotChangeAnother() {
                int stoppedId = createAllocation("ACTIVE");
                int untouchedId = createAllocation("ACTIVE");

                Map<String, Object> untouchedBefore = row(untouchedId);

                stop(stoppedId, EMAIL).statusCode(200);

                assertThat(status(stoppedId)).isEqualTo("PASSIVE");
                assertThat(row(untouchedId)).isEqualTo(untouchedBefore);
                assertThat(count()).isEqualTo(2L);
            }

            @Test
            void stoppingAllocationFreesParkingCapacity() {
                int firstId = createAllocation("ACTIVE");
                int secondId = createAllocation("ACTIVE");

                // The parking lot has capacity 2 and is initially full.
                post(request(PLATE, LOT)).statusCode(409);
                assertThat(count()).isEqualTo(2L);

                stop(firstId, EMAIL).statusCode(200);

                // A new allocation can now use the released spot.
                int newId = post(request(PLATE, LOT))
                        .statusCode(SUCCESS)
                        .body("status", equalTo("ACTIVE"))
                        .extract().path("id");

                assertThat(newId).isNotEqualTo(firstId).isNotEqualTo(secondId);
                assertThat(status(firstId)).isEqualTo("PASSIVE");
                assertThat(status(secondId)).isEqualTo("ACTIVE");

                assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM parkshark.parking_spot_allocation
                    WHERE parking_lot_id = ? AND status = 'ACTIVE'
                    """, Long.class, LOT)).isEqualTo(2L);

                // Stopping preserves history; it does not delete the allocation.
                assertThat(count()).isEqualTo(3L);
            }
        }

        @Nested
        class Ownership {

            @Test
            void anotherMemberCannotStopAllocation() {
                int id = createAllocation("ACTIVE");
                Map<String, Object> before = row(id);

                stop(id, OTHER_EMAIL).statusCode(403);

                assertThat(row(id)).isEqualTo(before);
                assertThat(count()).isEqualTo(1L);

                // A rejected attempt does not prevent the real owner from stopping.
                stop(id, EMAIL).statusCode(200);
                assertThat(status(id)).isEqualTo("PASSIVE");
            }
        }

        @Nested
        class InvalidState {

            @ParameterizedTest
            @ValueSource(strings = {"PASSIVE", "INVOICED"})
            void nonActiveAllocationCannotBeStopped(String initialStatus) {
                int id = createAllocation(initialStatus);
                Map<String, Object> before = row(id);

                stop(id, EMAIL).statusCode(409);

                // In particular, the original end time must not be overwritten.
                assertThat(row(id)).isEqualTo(before);
                assertThat(count()).isEqualTo(1L);
            }

            @Test
            void stoppingTwiceIsRejectedAndPreservesFirstStop() {
                int id = createAllocation("ACTIVE");

                stop(id, EMAIL).statusCode(200);
                Map<String, Object> afterFirstStop = row(id);

                stop(id, EMAIL).statusCode(409);

                assertThat(row(id)).isEqualTo(afterFirstStop);
                assertThat(count()).isEqualTo(1L);
            }
        }

        @Nested
        class InvalidAllocationId {

            @Test
            void unknownAllocationReturnsNotFound() {
                int existingId = createAllocation("ACTIVE");
                Map<String, Object> before = row(existingId);

                stop(999999, EMAIL).statusCode(404);

                assertThat(row(existingId)).isEqualTo(before);
                assertThat(count()).isEqualTo(1L);
            }

            @Test
            void nonNumericIdReturnsBadRequest() {
                stop("not-a-number", EMAIL).statusCode(400);

                assertThat(count()).isZero();
            }
        }

        private ValidatableResponse stop(int id, String email) {
            return stop(String.valueOf(id), email);
        }

        private ValidatableResponse stop(String id, String email) {
            return given()
                    .baseUri("http://localhost")
                    .port(port)
                    .auth().preemptive().basic(email, PASSWORD)
                    .contentType(ContentType.JSON)
                    .accept(ContentType.JSON)
                    .when()
                    .put("/allocation/stop/{id}", id)
                    .then()
                    .log().ifValidationFails();
        }

        private int createAllocation(String initialStatus) {
            // Reuse the outer class's SQL fixture helper.
            seedAllocation(LOT, initialStatus);

            // Each test runs sequentially in its dedicated H2 database.
            return jdbc.queryForObject("""
                SELECT MAX(id)
                FROM parkshark.parking_spot_allocation
                WHERE member_id = ? AND parking_lot_id = ?
                """, Integer.class, MEMBER, LOT);
        }

        private Map<String, Object> row(int id) {
            return jdbc.queryForMap("""
                SELECT *
                FROM parkshark.parking_spot_allocation
                WHERE id = ?
                """, id);
        }

        private String status(int id) {
            return jdbc.queryForObject("""
                SELECT status
                FROM parkshark.parking_spot_allocation
                WHERE id = ?
                """, String.class, id);
        }

        private OffsetDateTime timeColumn(int id, String column) {
            // Column names are fixed by test code, never supplied by an API caller.
            return jdbc.queryForObject(
                    "SELECT " + column
                            + " FROM parkshark.parking_spot_allocation WHERE id = ?",
                    OffsetDateTime.class,
                    id
            );
        }
    }

    @Nested
    class GetAllAllocations {

        private static final int OLDEST = 940101;
        private static final int STOPPED = 940102;
        private static final int INVOICED = 940103;
        private static final int NEWEST = 940104;

        @BeforeEach
        void createAllocationsForOverview() {
            // Insert deliberately out of chronological order.
            // Sorting by insertion order or ID must not accidentally pass.
            insertOverviewAllocation(NEWEST, OTHER_LOT, "ACTIVE", 1);
            insertOverviewAllocation(STOPPED, LOT, "PASSIVE", 3);
            insertOverviewAllocation(OLDEST, LOT, "ACTIVE", 4);
            insertOverviewAllocation(INVOICED, OTHER_LOT, "INVOICED", 2);
        }

        @Nested
        class DefaultOverview {

            @Test
            void withoutFiltersReturnsAllStatusesAndLots_oldestFirst() {
                overview(Map.of())
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", contains(
                                OLDEST, STOPPED, INVOICED, NEWEST
                        ))
                        .body("status", contains(
                                "ACTIVE", "PASSIVE", "INVOICED", "ACTIVE"
                        ))
                        .body("parkingLotId", contains(
                                LOT, LOT, OTHER_LOT, OTHER_LOT
                        ));
            }

            @Test
            void responseContainsAllocationDetails() {
                var response = overview(Map.of())
                        .statusCode(200)
                        .body("[0].id", equalTo(OLDEST))
                        .body("[0].memberId", equalTo(MEMBER))
                        .body("[0].parkingLotId", equalTo(LOT))
                        .body("[0].licensePlateNumber", equalTo(PLATE))
                        .body("[0].status", equalTo("ACTIVE"))
                        .body("[0].endTime", nullValue())
                        .body("[1].status", equalTo("PASSIVE"))
                        .body("[1].endTime", notNullValue())
                        .extract();

                assertThat(OffsetDateTime.parse(
                        response.path("[0].startTime")
                ).toInstant()).isEqualTo(NOW.minusSeconds(4 * 3600));

                assertThat(OffsetDateTime.parse(
                        response.path("[1].endTime")
                ).toInstant()).isEqualTo(
                        NOW.minusSeconds(3 * 3600).plusSeconds(1800)
                );
            }

            @Test
            void emptyDatabaseReturnsEmptyList() {
                jdbc.update("""
                    DELETE FROM parkshark.parking_spot_allocation
                    WHERE parking_lot_id IN (?, ?)
                    """, LOT, OTHER_LOT);

                overview(Map.of())
                        .statusCode(200)
                        .body("$", hasSize(0));
            }

            @Test
            void gettingOverviewDoesNotChangeDatabase() {
                List<Map<String, Object>> before = allocationRows();

                overview(Map.of(
                        "status", "ACTIVE",
                        "limit", 1,
                        "desc", true
                ))
                        .statusCode(200)
                        .body("id", contains(NEWEST));

                assertThat(allocationRows()).isEqualTo(before);
            }
        }

        @Nested
        class StatusFiltering {

            @Test
            void activeFilterReturnsOnlyActiveAllocations() {
                overview(Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("id", contains(OLDEST, NEWEST))
                        .body("status", everyItem(equalTo("ACTIVE")));
            }

            @Test
            void passiveFilterExcludesActiveAndInvoicedAllocations() {
                overview(Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("id", contains(STOPPED))
                        .body("status", contains("PASSIVE"));
            }

            @Test
            void invoicedFilterReturnsOnlyInvoicedAllocations() {
                overview(Map.of("status", "INVOICED"))
                        .statusCode(200)
                        .body("id", contains(INVOICED))
                        .body("status", contains("INVOICED"));
            }

            @Test
            void noMatchingStatusReturnsEmptyList() {
                jdbc.update("""
                    DELETE FROM parkshark.parking_spot_allocation
                    WHERE id = ?
                    """, STOPPED);

                overview(Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("$", hasSize(0));

                // Other allocations still exist.
                assertThat(count()).isEqualTo(3L);
            }
        }

        @Nested
        class Ordering {

            @Test
            void explicitAscendingReturnsOldestFirst() {
                overview(Map.of("desc", false))
                        .statusCode(200)
                        .body("id", contains(
                                OLDEST, STOPPED, INVOICED, NEWEST
                        ));
            }

            @Test
            void descendingReturnsNewestFirst() {
                overview(Map.of("desc", true))
                        .statusCode(200)
                        .body("id", contains(
                                NEWEST, INVOICED, STOPPED, OLDEST
                        ));
            }

            @Test
            void descendingAlsoWorksWithStatusFilter() {
                overview(Map.of(
                        "status", "ACTIVE",
                        "desc", true
                ))
                        .statusCode(200)
                        .body("id", contains(NEWEST, OLDEST));
            }

            @Test
            void equalStartTimesAreOrderedByIdAscending() {
                giveActiveAllocationsSameStartTime();

                overview(Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("id", contains(OLDEST, NEWEST));
            }

            @Test
            void equalStartTimesAreOrderedByIdDescending() {
                giveActiveAllocationsSameStartTime();

                overview(Map.of(
                        "status", "ACTIVE",
                        "desc", true
                ))
                        .statusCode(200)
                        .body("id", contains(NEWEST, OLDEST));
            }

            private void giveActiveAllocationsSameStartTime() {
                jdbc.update("""
                    UPDATE parkshark.parking_spot_allocation
                    SET start_time = ?
                    WHERE id IN (?, ?)
                    """,
                        OffsetDateTime.ofInstant(
                                NOW.minusSeconds(3600),
                                ZoneOffset.UTC
                        ),
                        OLDEST,
                        NEWEST);
            }
        }

        @Nested
        class Limiting {

            @Test
            void limitOneReturnsFirstAllocation() {
                overview(Map.of("limit", 1))
                        .statusCode(200)
                        .body("id", contains(OLDEST));
            }

            @Test
            void limitIsAppliedAfterSorting() {
                overview(Map.of(
                        "limit", 2,
                        "desc", true
                ))
                        .statusCode(200)
                        .body("id", contains(NEWEST, INVOICED));
            }

            @Test
            void limitEqualToResultCountReturnsAllResults() {
                overview(Map.of("limit", 4))
                        .statusCode(200)
                        .body("id", contains(
                                OLDEST, STOPPED, INVOICED, NEWEST
                        ));
            }

            @Test
            void limitLargerThanResultCountReturnsAvailableResults() {
                overview(Map.of("limit", 100))
                        .statusCode(200)
                        .body("id", contains(
                                OLDEST, STOPPED, INVOICED, NEWEST
                        ));
            }

            @Test
            void filteringHappensBeforeLimiting() {
                // If the implementation limits first, STOPPED could occupy
                // the second slot and incorrectly exclude NEWEST.
                overview(Map.of(
                        "status", "ACTIVE",
                        "limit", 2
                ))
                        .statusCode(200)
                        .body("id", contains(OLDEST, NEWEST));
            }

            @Test
            void allThreeParametersWorkTogether() {
                overview(Map.of(
                        "status", "ACTIVE",
                        "limit", 1,
                        "desc", true
                ))
                        .statusCode(200)
                        .body("id", contains(NEWEST))
                        .body("status", contains("ACTIVE"));
            }
        }

        @Nested
        class InvalidParameters {

            @ParameterizedTest
            @ValueSource(ints = {0, -1, -100})
            void nonPositiveLimitReturnsBadRequest(int limit) {
                assertRejectedWithoutChanges(Map.of("limit", limit));
            }

            @ParameterizedTest
            @ValueSource(strings = {"abc", "1.5", "2147483648"})
            void invalidIntegerLimitReturnsBadRequest(String limit) {
                assertRejectedWithoutChanges(Map.of("limit", limit));
            }

            @Test
            void unknownStatusReturnsBadRequest() {
                assertRejectedWithoutChanges(Map.of("status", "UNKNOWN"));
            }

            @Test
            void invalidBooleanReturnsBadRequest() {
                assertRejectedWithoutChanges(Map.of("desc", "invalid"));
            }

            private void assertRejectedWithoutChanges(
                    Map<String, Object> parameters
            ) {
                List<Map<String, Object>> before = allocationRows();

                overview(parameters).statusCode(400);

                assertThat(allocationRows()).isEqualTo(before);
            }
        }

        private ValidatableResponse overview(
                Map<String, Object> parameters
        ) {
            // Reuse the manager created by the outer setUp().
            String managerEmail = jdbc.queryForObject(
                    "SELECT email FROM parkshark.person WHERE id = ?",
                    String.class,
                    MANAGER
            );

            return given()
                    .baseUri("http://localhost")
                    .port(port)
                    .auth().preemptive().basic(managerEmail, PASSWORD)
                    .accept(ContentType.JSON)
                    .queryParams(parameters)
                    .when()
                    .get("/allocation")
                    .then()
                    .log().ifValidationFails();
        }

        private void insertOverviewAllocation(
                int id,
                int lotId,
                String status,
                int hoursAgo
        ) {
            OffsetDateTime start = OffsetDateTime.ofInstant(
                    NOW.minusSeconds(hoursAgo * 3600L),
                    ZoneOffset.UTC
            );

            OffsetDateTime end = status.equals("ACTIVE")
                    ? null
                    : start.plusMinutes(30);

            jdbc.update("""
                INSERT INTO parkshark.parking_spot_allocation (
                    id, member_id, parking_lot_id,
                    license_plate_number, start_time, end_time, status
                )
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                    id,
                    MEMBER,
                    lotId,
                    PLATE,
                    start,
                    end,
                    status);
        }

        private List<Map<String, Object>> allocationRows() {
            return jdbc.queryForList("""
                SELECT *
                FROM parkshark.parking_spot_allocation
                WHERE parking_lot_id IN (?, ?)
                ORDER BY id
                """, LOT, OTHER_LOT);
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
