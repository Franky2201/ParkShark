package com.bordeauxbrothers.parkshark.allocation.api;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.bordeauxbrothers.parkshark.allocation.api.AllocationTestData.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * PSA1–PSA4 functional end-to-end tests against a dedicated H2 database.
 * Shared SQL fixtures live in AllocationTestData; assertions stay with each story.
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
    private AllocationTestData data;

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
        data = new AllocationTestData(jdbc, passwordEncoder);
        data.reset();
    }

    @AfterEach
    void tearDown() {
        if (data != null) data.cleanUp();
    }

    @Nested
    class StartAllocation {
        @Nested
        class SuccessfulAllocation {
            @ParameterizedTest
            @ValueSource(strings = {"BRONZE", "SILVER", "GOLD"})
            void matchingPlate_createsActiveAllocationAndPersistsResponse(String membership) {
                data.membership(membership);
                int id = post(request(PLATE, LOT)).statusCode(CREATED)
                        .body("id", greaterThan(0))
                        .body("memberId", equalTo(MEMBER))
                        .body("parkingLotId", equalTo(LOT))
                        .body("licensePlateNumber", equalTo(PLATE))
                        .body("status", equalTo("ACTIVE"))
                        .body("endTime", nullValue())
                        .extract().path("id");
                assertThat(data.count()).isEqualTo(1L);
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
                String start = post(request(PLATE, LOT)).statusCode(CREATED)
                        .extract().path("startTime");
                assertThat(OffsetDateTime.parse(start).toInstant()).isEqualTo(NOW);
            }

            @Test
            void goldCanParkAnotherCar_andActualPlateIsStored() {
                data.membership("GOLD");
                int id = post(request("RENTAL-123", LOT)).statusCode(CREATED)
                        .body("licensePlateNumber", equalTo("RENTAL-123"))
                        .extract().path("id");
                assertThat(jdbc.queryForObject(
                        "SELECT license_plate_number FROM parkshark.parking_spot_allocation WHERE id = ?",
                        String.class, id)).isEqualTo("RENTAL-123");
            }

            @Test
            void generatedIdsAreUnique() {
                int first = post(request(PLATE, LOT)).statusCode(CREATED).extract().path("id");
                // End first session before starting another; no assumption about simultaneous sessions.
                jdbc.update("UPDATE parkshark.parking_spot_allocation SET status = 'PASSIVE', end_time = start_time WHERE id = ?", first);
                int second = post(request(PLATE, LOT)).statusCode(CREATED).extract().path("id");
                assertThat(second).isNotEqualTo(first);
                assertThat(data.count()).isEqualTo(2L);
            }
        }

        @Nested
        class BusinessRules {
            @ParameterizedTest
            @ValueSource(strings = {"BRONZE", "SILVER"})
            void differentPlateIsRejected(String membership) {
                data.membership(membership);
                post(request("OTHER-123", LOT)).statusCode(400);
                assertThat(data.count()).isZero();
            }

            @Test
            void unknownLotIsRejected() {
                post(request(PLATE, 999999)).statusCode(404);
                assertThat(data.count()).isZero();
            }

            @Test
            void exactlyFullLotIsRejected() {
                data.allocation(LOT, "ACTIVE");
                data.allocation(LOT, "ACTIVE");
                post(request(PLATE, LOT)).statusCode(409);
                assertThat(data.count()).isEqualTo(2L);
            }

            @Test
            void lastAvailableSpotCanBeTaken_butNextRequestIsRejected() {
                data.allocation(LOT, "ACTIVE");
                post(request(PLATE, LOT)).statusCode(CREATED);
                assertThat(data.count()).isEqualTo(2L);
                post(request(PLATE, LOT)).statusCode(409);
                assertThat(data.count()).isEqualTo(2L);
            }

            @Test
            void allocationsInOtherLotsDoNotConsumeRequestedLotCapacity() {
                data.allocation(OTHER_LOT, "ACTIVE");
                data.allocation(OTHER_LOT, "ACTIVE");
                post(request(PLATE, LOT)).statusCode(CREATED).body("parkingLotId", equalTo(LOT));
                assertThat(data.count()).isEqualTo(3L);
            }

            @Test
            void stoppedAndInvoicedAllocationsDoNotConsumeCapacity() {
                data.allocation(LOT, "PASSIVE");
                data.allocation(LOT, "INVOICED");
                post(request(PLATE, LOT)).statusCode(CREATED);
                assertThat(data.count()).isEqualTo(3L);
            }
        }

        @Nested
        class RequestValidation {
            @Test
            void missingPlateIsRejected() {
                Map<String, Object> body = request(PLATE, LOT);
                body.remove("licensePlate");
                post(body).statusCode(400);
                assertThat(data.count()).isZero();
            }

            @ParameterizedTest
            @ValueSource(strings = {"", "   "})
            void blankPlateIsRejectedEvenForGold(String plate) {
                data.membership("GOLD");
                post(request(plate, LOT)).statusCode(400);
                assertThat(data.count()).isZero();
            }

            @Test
            void missingPlateNumberIsRejectedEvenForGold() {
                data.membership("GOLD");
                Map<String, Object> body = request(PLATE, LOT);
                body.put("licensePlate", Map.of("country", "BE"));
                post(body).statusCode(400);
                assertThat(data.count()).isZero();
            }

            @Test
            void missingParkingLotIdIsRejected() {
                Map<String, Object> body = request(PLATE, LOT);
                body.remove("parkingLotId");
                post(body).statusCode(400);
                assertThat(data.count()).isZero();
            }

            @ParameterizedTest
            @ValueSource(ints = {0, -1})
            void nonPositiveParkingLotIdIsRejected(int lotId) {
                post(request(PLATE, lotId)).statusCode(400);
                assertThat(data.count()).isZero();
            }
        }
    }

    @Nested
    class StopAllocation {

        private static final String OTHER_EMAIL = "other@psa2-test.example";

        @BeforeEach
        void createOtherMember() {
            data.member(OTHER_MEMBER, OTHER_EMAIL, "PSA2-OTHER");
        }

        @Nested
        class SuccessfulStop {

            @ParameterizedTest
            @ValueSource(strings = {"BRONZE", "SILVER", "GOLD"})
            void ownerCanStopActiveAllocation(String membership) {
                data.membership(membership);
                int id = data.allocation("ACTIVE");
                OffsetDateTime originalStart = data.time(id, "start_time");

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
                assertThat(data.status(id)).isEqualTo("PASSIVE");
                assertThat(data.time(id, "end_time").toInstant()).isEqualTo(NOW);
                assertThat(data.time(id, "start_time")).isEqualTo(originalStart);
                assertThat(data.count()).isEqualTo(1L);
            }

            @Test
            void stoppingOneAllocationDoesNotChangeAnother() {
                int stoppedId = data.allocation("ACTIVE");
                int untouchedId = data.allocation("ACTIVE");

                Map<String, Object> untouchedBefore = data.row(untouchedId);

                stop(stoppedId, EMAIL).statusCode(200);

                assertThat(data.status(stoppedId)).isEqualTo("PASSIVE");
                assertThat(data.row(untouchedId)).isEqualTo(untouchedBefore);
                assertThat(data.count()).isEqualTo(2L);
            }

            @Test
            void stoppingAllocationFreesParkingCapacity() {
                int firstId = data.allocation("ACTIVE");
                int secondId = data.allocation("ACTIVE");

                // The parking lot has capacity 2 and is initially full.
                post(request(PLATE, LOT)).statusCode(409);
                assertThat(data.count()).isEqualTo(2L);

                stop(firstId, EMAIL).statusCode(200);

                // A new allocation can now use the released spot.
                int newId = post(request(PLATE, LOT))
                        .statusCode(CREATED)
                        .body("status", equalTo("ACTIVE"))
                        .extract().path("id");

                assertThat(newId).isNotEqualTo(firstId).isNotEqualTo(secondId);
                assertThat(data.status(firstId)).isEqualTo("PASSIVE");
                assertThat(data.status(secondId)).isEqualTo("ACTIVE");

                assertThat(jdbc.queryForObject("""
                    SELECT COUNT(*)
                    FROM parkshark.parking_spot_allocation
                    WHERE parking_lot_id = ? AND status = 'ACTIVE'
                    """, Long.class, LOT)).isEqualTo(2L);

                // Stopping preserves history; it does not delete the allocation.
                assertThat(data.count()).isEqualTo(3L);
            }
        }

        @Nested
        class Ownership {

            @Test
            void anotherMemberCannotStopAllocation() {
                int id = data.allocation("ACTIVE");
                Map<String, Object> before = data.row(id);

                stop(id, OTHER_EMAIL).statusCode(403);

                assertThat(data.row(id)).isEqualTo(before);
                assertThat(data.count()).isEqualTo(1L);

                // A rejected attempt does not prevent the real owner from stopping.
                stop(id, EMAIL).statusCode(200);
                assertThat(data.status(id)).isEqualTo("PASSIVE");
            }
        }

        @Nested
        class InvalidState {

            @ParameterizedTest
            @ValueSource(strings = {"PASSIVE", "INVOICED"})
            void nonActiveAllocationCannotBeStopped(String initialStatus) {
                int id = data.allocation(initialStatus);
                Map<String, Object> before = data.row(id);

                stop(id, EMAIL).statusCode(409);

                // In particular, the original end time must not be overwritten.
                assertThat(data.row(id)).isEqualTo(before);
                assertThat(data.count()).isEqualTo(1L);
            }

            @Test
            void stoppingTwiceIsRejectedAndPreservesFirstStop() {
                int id = data.allocation("ACTIVE");

                stop(id, EMAIL).statusCode(200);
                Map<String, Object> afterFirstStop = data.row(id);

                stop(id, EMAIL).statusCode(409);

                assertThat(data.row(id)).isEqualTo(afterFirstStop);
                assertThat(data.count()).isEqualTo(1L);
            }
        }

        @Nested
        class InvalidAllocationId {

            @Test
            void unknownAllocationReturnsNotFound() {
                int existingId = data.allocation("ACTIVE");
                Map<String, Object> before = data.row(existingId);

                stop(999999, EMAIL).statusCode(404);

                assertThat(data.row(existingId)).isEqualTo(before);
                assertThat(data.count()).isEqualTo(1L);
            }

            @Test
            void nonNumericIdReturnsBadRequest() {
                stop("not-a-number", EMAIL).statusCode(400);

                assertThat(data.count()).isZero();
            }
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
            data.allocation(NEWEST, OTHER_LOT, "ACTIVE", 1);
            data.allocation(STOPPED, LOT, "PASSIVE", 3);
            data.allocation(OLDEST, LOT, "ACTIVE", 4);
            data.allocation(INVOICED, OTHER_LOT, "INVOICED", 2);
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
                List<Map<String, Object>> before = data.rows();

                overview(Map.of(
                        "status", "ACTIVE",
                        "limit", 1,
                        "desc", true
                ))
                        .statusCode(200)
                        .body("id", contains(NEWEST));

                assertThat(data.rows()).isEqualTo(before);
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
                assertThat(data.count()).isEqualTo(3L);
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
                List<Map<String, Object>> before = data.rows();

                overview(parameters).statusCode(400);

                assertThat(data.rows()).isEqualTo(before);
            }
        }

    }

    @Nested
    class GetAllocationsForMember {


        private static final int MEMBER_ACTIVE = 950101;
        private static final int MEMBER_PASSIVE = 950102;
        private static final int MEMBER_INVOICED = 950103;

        private static final int SECOND_ACTIVE = 950104;
        private static final int SECOND_PASSIVE = 950105;
        private static final int SECOND_INVOICED = 950106;

        @BeforeEach
        void createMemberAllocations() {
            data.member(SECOND_MEMBER, "second@psa4-test.example", "PSA4-SECOND");
            // Both members have every status, including allocations
            // in the same parking lot. This exposes missing member filters.
            data.allocation(
                    MEMBER_ACTIVE, MEMBER, LOT, "ACTIVE", PLATE, 1
            );
            data.allocation(
                    MEMBER_PASSIVE, MEMBER, OTHER_LOT, "PASSIVE", PLATE, 3
            );
            data.allocation(
                    MEMBER_INVOICED, MEMBER, LOT, "INVOICED", PLATE, 5
            );

            data.allocation(
                    SECOND_ACTIVE, SECOND_MEMBER, LOT,
                    "ACTIVE", "PSA4-SECOND", 2
            );
            data.allocation(
                    SECOND_PASSIVE, SECOND_MEMBER, OTHER_LOT,
                    "PASSIVE", "PSA4-SECOND", 4
            );
            data.allocation(
                    SECOND_INVOICED, SECOND_MEMBER, LOT,
                    "INVOICED", "PSA4-SECOND", 6
            );
                }

        @Nested
        class MemberSelection {

            @Test
            void withoutStatusReturnsAllAllocationsOfRequestedMember() {
                forMember(MEMBER, Map.of())
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", containsInAnyOrder(
                                MEMBER_ACTIVE,
                                MEMBER_PASSIVE,
                                MEMBER_INVOICED
                        ))
                        .body("memberId", everyItem(equalTo(MEMBER)))
                        .body("status", containsInAnyOrder(
                                "ACTIVE", "PASSIVE", "INVOICED"
                        ));
            }

            @Test
            void requestingAnotherMemberReturnsThatMembersAllocations() {
                forMember(SECOND_MEMBER, Map.of())
                        .statusCode(200)
                        .body("id", containsInAnyOrder(
                                SECOND_ACTIVE,
                                SECOND_PASSIVE,
                                SECOND_INVOICED
                        ))
                        .body(
                                "memberId",
                                everyItem(equalTo(SECOND_MEMBER))
                        );
            }

            @Test
            void allocationsAcrossDifferentParkingLotsAreIncluded() {
                forMember(MEMBER, Map.of())
                        .statusCode(200)
                        .body("id", containsInAnyOrder(
                                MEMBER_ACTIVE,
                                MEMBER_PASSIVE,
                                MEMBER_INVOICED
                        ))
                        .body("parkingLotId", containsInAnyOrder(
                                LOT, OTHER_LOT, LOT
                        ));
            }

            @Test
            void existingMemberWithoutAllocationsGetsEmptyList() {
                jdbc.update("""
                    DELETE FROM parkshark.parking_spot_allocation
                    WHERE member_id = ?
                    """, MEMBER);

                forMember(MEMBER, Map.of())
                        .statusCode(200)
                        .body("$", hasSize(0));

                // The second member's allocations must still exist.
                assertThat(data.count()).isEqualTo(3L);
            }

            @Test
            void unknownMemberGetsEmptyListWithCurrentContract() {
                forMember(999999, Map.of())
                        .statusCode(200)
                        .body("$", hasSize(0));

                assertThat(data.count()).isEqualTo(6L);
            }
        }

        @Nested
        class StatusFiltering {

            @Test
            void activeFilterReturnsOnlyRequestedMembersActiveAllocations() {
                forMember(MEMBER, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("id", contains(MEMBER_ACTIVE))
                        .body("memberId", contains(MEMBER))
                        .body("status", contains("ACTIVE"));
            }

            @Test
            void passiveFilterExcludesInvoicedAndOtherMembersAllocations() {
                forMember(MEMBER, Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("id", contains(MEMBER_PASSIVE))
                        .body("memberId", contains(MEMBER))
                        .body("status", contains("PASSIVE"));
            }

            @Test
            void invoicedFilterReturnsOnlyRequestedMembersInvoicedAllocations() {
                forMember(MEMBER, Map.of("status", "INVOICED"))
                        .statusCode(200)
                        .body("id", contains(MEMBER_INVOICED))
                        .body("memberId", contains(MEMBER))
                        .body("status", contains("INVOICED"));
            }

            @Test
            void statusFilterAlsoWorksForSecondMember() {
                forMember(SECOND_MEMBER, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("id", contains(SECOND_ACTIVE))
                        .body("memberId", contains(SECOND_MEMBER));
            }

            @Test
            void noMatchingStatusReturnsEmptyList_evenWhenAnotherMemberMatches() {
                jdbc.update("""
                    DELETE FROM parkshark.parking_spot_allocation
                    WHERE id = ?
                    """, MEMBER_PASSIVE);

                forMember(MEMBER, Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("$", hasSize(0));

                // A PASSIVE allocation exists, but belongs to another member.
                forMember(SECOND_MEMBER, Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("id", contains(SECOND_PASSIVE));
            }

            @Test
            void unknownMemberWithStatusGetsEmptyList() {
                forMember(999999, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("$", hasSize(0));
            }
        }

        @Nested
        class ResponseAndPersistence {

            @Test
            void responseContainsCorrectAllocationDetails() {
                var response = forMember(MEMBER, Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("$", hasSize(1))
                        .body("[0].id", equalTo(MEMBER_PASSIVE))
                        .body("[0].memberId", equalTo(MEMBER))
                        .body("[0].parkingLotId", equalTo(OTHER_LOT))
                        .body("[0].licensePlateNumber", equalTo(PLATE))
                        .body("[0].status", equalTo("PASSIVE"))
                        .extract();

                assertThat(OffsetDateTime.parse(
                        response.path("[0].startTime")
                ).toInstant()).isEqualTo(NOW.minusSeconds(3 * 3600));

                assertThat(OffsetDateTime.parse(
                        response.path("[0].endTime")
                ).toInstant()).isEqualTo(
                        NOW.minusSeconds(3 * 3600).plusSeconds(1800)
                );
            }

            @Test
            void activeAllocationHasNoEndTime() {
                forMember(MEMBER, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("$", hasSize(1))
                        .body("[0].id", equalTo(MEMBER_ACTIVE))
                        .body("[0].startTime", notNullValue())
                        .body("[0].endTime", nullValue());
            }

            @Test
            void readingAllocationsDoesNotChangeEitherMembersData() {
                List<Map<String, Object>> before = data.rows();

                forMember(MEMBER, Map.of()).statusCode(200);
                forMember(SECOND_MEMBER, Map.of("status", "PASSIVE"))
                        .statusCode(200);

                assertThat(data.rows()).isEqualTo(before);
            }
        }

        @Nested
        class InvalidParameters {

            @Test
            void unknownStatusReturnsBadRequestWithoutChangingData() {
                List<Map<String, Object>> before = data.rows();

                forMember(MEMBER, Map.of("status", "UNKNOWN"))
                        .statusCode(400);

                assertThat(data.rows()).isEqualTo(before);
            }

            @ParameterizedTest
            @ValueSource(strings = {"abc", "1.5", "2147483648"})
            void invalidIntegerMemberIdReturnsBadRequest(String memberId) {
                List<Map<String, Object>> before = data.rows();

                forMember(memberId, Map.of())
                        .statusCode(400);

                assertThat(data.rows()).isEqualTo(before);
            }
        }

    }

    @Nested
    class GetAllocationsForParkingLot {

        private static final int LOT_ACTIVE = 960101;
        private static final int LOT_PASSIVE = 960102;
        private static final int LOT_INVOICED = 960103;

        private static final int OTHER_ACTIVE = 960104;
        private static final int OTHER_PASSIVE = 960105;
        private static final int OTHER_INVOICED = 960106;

        @BeforeEach
        void createParkingLotAllocations() {
            data.member(
                    SECOND_MEMBER,
                    "second@psa5-test.example",
                    "PSA5-SECOND"
            );

            // Both parking lots contain every status.
            // Each parking lot also contains allocations from different members.
            data.allocation(
                    LOT_ACTIVE, MEMBER, LOT, "ACTIVE", PLATE, 1
            );
            data.allocation(
                    LOT_PASSIVE, SECOND_MEMBER, LOT,
                    "PASSIVE", "PSA5-SECOND", 3
            );
            data.allocation(
                    LOT_INVOICED, MEMBER, LOT, "INVOICED", PLATE, 5
            );

            data.allocation(
                    OTHER_ACTIVE, SECOND_MEMBER, OTHER_LOT,
                    "ACTIVE", "PSA5-SECOND", 2
            );
            data.allocation(
                    OTHER_PASSIVE, MEMBER, OTHER_LOT,
                    "PASSIVE", PLATE, 4
            );
            data.allocation(
                    OTHER_INVOICED, SECOND_MEMBER, OTHER_LOT,
                    "INVOICED", "PSA5-SECOND", 6
            );
        }

        @Nested
        class ParkingLotSelection {

            @Test
            void withoutStatusReturnsAllAllocationsForRequestedParkingLot() {
                forParkingLot(LOT, Map.of())
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("id", containsInAnyOrder(
                                LOT_ACTIVE, LOT_PASSIVE, LOT_INVOICED
                        ))
                        .body("parkingLotId", everyItem(equalTo(LOT)))
                        .body("status", containsInAnyOrder(
                                "ACTIVE", "PASSIVE", "INVOICED"
                        ));
            }

            @Test
            void requestingOtherParkingLotReturnsItsAllocations() {
                forParkingLot(OTHER_LOT, Map.of())
                        .statusCode(200)
                        .body("id", containsInAnyOrder(
                                OTHER_ACTIVE, OTHER_PASSIVE, OTHER_INVOICED
                        ))
                        .body(
                                "parkingLotId",
                                everyItem(equalTo(OTHER_LOT))
                        );
            }

            @Test
            void includesAllocationsFromDifferentMembers() {
                forParkingLot(LOT, Map.of())
                        .statusCode(200)
                        .body("id", containsInAnyOrder(
                                LOT_ACTIVE, LOT_PASSIVE, LOT_INVOICED
                        ))
                        .body("memberId", containsInAnyOrder(
                                MEMBER, SECOND_MEMBER, MEMBER
                        ));
            }

            @Test
            void parkingLotWithoutAllocationsReturnsEmptyList() {
                jdbc.update("""
                    DELETE FROM parkshark.parking_spot_allocation
                    WHERE parking_lot_id = ?
                    """, LOT);

                forParkingLot(LOT, Map.of())
                        .statusCode(200)
                        .body("$", hasSize(0));

                // The other parking lot still has allocations.
                assertThat(data.count()).isEqualTo(3L);
            }

            @Test
            void unknownParkingLotReturnsEmptyListWithCurrentContract() {
                forParkingLot(999999, Map.of())
                        .statusCode(200)
                        .body("$", hasSize(0));

                assertThat(data.count()).isEqualTo(6L);
            }
        }

        @Nested
        class StatusFiltering {

            @Test
            void activeFilterReturnsOnlyActiveAllocationsOfRequestedLot() {
                forParkingLot(LOT, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("id", contains(LOT_ACTIVE))
                        .body("parkingLotId", contains(LOT))
                        .body("status", contains("ACTIVE"));
            }

            @Test
            void passiveFilterExcludesInvoicedAndOtherLotsAllocations() {
                forParkingLot(LOT, Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("id", contains(LOT_PASSIVE))
                        .body("parkingLotId", contains(LOT))
                        .body("status", contains("PASSIVE"));
            }

            @Test
            void invoicedFilterReturnsOnlyInvoicedAllocationsOfRequestedLot() {
                forParkingLot(LOT, Map.of("status", "INVOICED"))
                        .statusCode(200)
                        .body("id", contains(LOT_INVOICED))
                        .body("parkingLotId", contains(LOT))
                        .body("status", contains("INVOICED"));
            }

            @Test
            void statusFilterAlsoWorksForOtherParkingLot() {
                forParkingLot(OTHER_LOT, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("id", contains(OTHER_ACTIVE))
                        .body("parkingLotId", contains(OTHER_LOT));
            }

            @Test
            void noMatchingStatusReturnsEmptyList_evenWhenOtherLotMatches() {
                jdbc.update("""
                    DELETE FROM parkshark.parking_spot_allocation
                    WHERE id = ?
                    """, LOT_PASSIVE);

                forParkingLot(LOT, Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("$", hasSize(0));

                forParkingLot(OTHER_LOT, Map.of("status", "PASSIVE"))
                        .statusCode(200)
                        .body("id", contains(OTHER_PASSIVE));
            }

            @Test
            void unknownParkingLotWithStatusReturnsEmptyList() {
                forParkingLot(999999, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("$", hasSize(0));
            }
        }

        @Nested
        class ResponseAndPersistence {

            @Test
            void responseContainsCorrectAllocationDetails() {
                var response = forParkingLot(
                        LOT, Map.of("status", "PASSIVE")
                )
                        .statusCode(200)
                        .contentType(ContentType.JSON)
                        .body("$", hasSize(1))
                        .body("[0].id", equalTo(LOT_PASSIVE))
                        .body("[0].memberId", equalTo(SECOND_MEMBER))
                        .body("[0].parkingLotId", equalTo(LOT))
                        .body("[0].licensePlateNumber", equalTo("PSA5-SECOND"))
                        .body("[0].status", equalTo("PASSIVE"))
                        .extract();

                assertThat(OffsetDateTime.parse(
                        response.path("[0].startTime")
                ).toInstant()).isEqualTo(NOW.minusSeconds(3 * 3600));

                assertThat(OffsetDateTime.parse(
                        response.path("[0].endTime")
                ).toInstant()).isEqualTo(
                        NOW.minusSeconds(3 * 3600).plusSeconds(1800)
                );
            }

            @Test
            void activeAllocationHasNoEndTime() {
                forParkingLot(LOT, Map.of("status", "ACTIVE"))
                        .statusCode(200)
                        .body("$", hasSize(1))
                        .body("[0].id", equalTo(LOT_ACTIVE))
                        .body("[0].startTime", notNullValue())
                        .body("[0].endTime", nullValue());
            }

            @Test
            void readingAllocationsDoesNotChangeDatabase() {
                var before = data.rows();

                forParkingLot(LOT, Map.of()).statusCode(200);
                forParkingLot(OTHER_LOT, Map.of("status", "PASSIVE"))
                        .statusCode(200);

                assertThat(data.rows()).isEqualTo(before);
            }
        }

        @Nested
        class InvalidParameters {

            @Test
            void unknownStatusReturnsBadRequestWithoutChangingData() {
                var before = data.rows();

                forParkingLot(LOT, Map.of("status", "UNKNOWN"))
                        .statusCode(400);

                assertThat(data.rows()).isEqualTo(before);
            }

            @ParameterizedTest
            @ValueSource(strings = {"abc", "1.5", "2147483648"})
            void invalidIntegerParkingLotIdReturnsBadRequest(String lotId) {
                var before = data.rows();

                forParkingLot(lotId, Map.of())
                        .statusCode(400);

                assertThat(data.rows()).isEqualTo(before);
            }
        }
    }

    private Map<String, Object> request(String plate, int lot) {
        Map<String, Object> body = new HashMap<>();
        body.put("licensePlate", Map.of("number", plate, "country", "BE"));
        body.put("parkingLotId", lot);
        return body;
    }

    // Shared HTTP helpers: no expectations here; each test owns its assertions.
    private RequestSpecification apiAs(String email) {
        return given().baseUri("http://localhost").port(port)
                .auth().preemptive().basic(email, PASSWORD)
                .accept(ContentType.JSON);
    }

    private ValidatableResponse post(Map<String, Object> body) {
        return apiAs(EMAIL).contentType(ContentType.JSON).body(body)
                .when().post("/allocation").then().log().ifValidationFails();
    }

    private ValidatableResponse stop(Object id, String email) {
        return apiAs(email).when().put("/allocation/stop/{id}", id)
                .then().log().ifValidationFails();
    }

    private ValidatableResponse overview(Map<String, Object> parameters) {
        return apiAs(MANAGER_EMAIL).queryParams(parameters).when().get("/allocation")
                .then().log().ifValidationFails();
    }

    private ValidatableResponse forMember(Object memberId, Map<String, Object> parameters) {
        return apiAs(MANAGER_EMAIL).queryParams(parameters).when()
                .get("/allocation/member/{id}", memberId).then().log().ifValidationFails();
    }

    private ValidatableResponse forParkingLot(
            Object parkingLotId,
            Map<String, Object> parameters
    ) {
        return apiAs(MANAGER_EMAIL)
                .queryParams(parameters)
                .when()
                .get("/allocation/parkinglot/{id}", parkingLotId)
                .then()
                .log().ifValidationFails();
    }
}
