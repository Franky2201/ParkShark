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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import com.bordeauxbrothers.parkshark.person.domain.Manager;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;

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
@Execution(ExecutionMode.SAME_THREAD)
class DivisionEndToEndTest {

    private static final int DIRECTOR_ID = 900001;
    private static final int SECOND_DIRECTOR_ID = 900002;

    private static final int MEMBER_ID = 900003;
    private static final int MEMBER_ADDRESS_ID = 900003;
    private static final int MEMBER_POSTAL_CODE_ID = 900003;
    private static final int MEMBER_PLATE_ID = 900003;
    private static final String MANAGER_EMAIL =
            "director-" + DIRECTOR_ID + "@division-test.example";
    private static final String MEMBER_EMAIL = "arya@division-test.example";
    private static final String TEST_PASSWORD = "Winterfell123!";

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PersonRepository personRepository;

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
        insertMember();
    }

    @AfterEach
    void cleanUp() {
        assertTestDatabase();
        deleteTestData();
    }

    @Test
    void seededUsers_canBeLoadedAsTheirConcretePersonTypes() {
        Person manager = personRepository.findByEmail(MANAGER_EMAIL)
                .orElseThrow(() ->
                        new AssertionError("Manager-testgebruiker ontbreekt"));

        Person member = personRepository.findByEmail(MEMBER_EMAIL)
                .orElseThrow(() ->
                        new AssertionError("Member-testgebruiker ontbreekt"));

        assertThat(manager).isInstanceOf(Manager.class);
        assertThat(member).isInstanceOf(Member.class);

        assertThat(passwordEncoder.matches(
                TEST_PASSWORD, manager.getPasswordHash()
        )).isTrue();

        assertThat(passwordEncoder.matches(
                TEST_PASSWORD, member.getPasswordHash()
        )).isTrue();
    }

    @Nested
    class CreateDivision {

        @Test
        void asManager_canAssignAnotherDirector() {
            api().contentType(ContentType.JSON)
                    .body(createRequest(NAME, ORIGINAL_NAME, SECOND_DIRECTOR_ID))
                    .when().post("/division")
                    .then().log().ifValidationFails()
                    .statusCode(201)
                    .body("directorId", equalTo(SECOND_DIRECTOR_ID));
            assertThat(jdbcTemplate.queryForObject(
                    "SELECT director_id FROM parkshark.division WHERE name = ?",
                    Integer.class, NAME)).isEqualTo(SECOND_DIRECTOR_ID);
        }


        @Nested
        class AccessControl {

            @Test
            void withoutCredentials_returnsUnauthorized() {
                assertCreateDenied(apiWithoutAuthentication(), 401);
            }

            @Test
            void withWrongPassword_returnsUnauthorized() {
                assertCreateDenied(apiAs(MANAGER_EMAIL, "incorrect-password"), 401);
            }

            @Test
            void withUnknownEmail_returnsUnauthorized() {
                assertCreateDenied(apiAs("missing@division-test.example", TEST_PASSWORD), 401);
            }

            @ParameterizedTest
            @ValueSource(strings = {"Bearer token", "Basic !!!", "Basic bm9jb2xvbg=="})
            void withMalformedHeader_returnsUnauthorized(String header) {
                assertCreateDenied(apiWithoutAuthentication()
                        .header("Authorization", header), 401);
            }

            @Test
            void asMember_returnsForbidden() {
                // Correct credentials, but no manager permissions.
                assertCreateDenied(apiAs(MEMBER_EMAIL, TEST_PASSWORD), 403);
            }
        }


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

        @Nested
        class AccessControl {

            @Test
            void withoutCredentials_returnsUnauthorized() {
                assertGetDenied(apiWithoutAuthentication(), 401);
            }

            @Test
            void withWrongPassword_returnsUnauthorized() {
                assertGetDenied(apiAs(MANAGER_EMAIL, "incorrect-password"), 401);
            }

            @Test
            void withUnknownEmail_returnsUnauthorized() {
                assertGetDenied(apiAs("missing@division-test.example", TEST_PASSWORD), 401);
            }

            @ParameterizedTest
            @ValueSource(strings = {"Bearer token", "Basic !!!", "Basic bm9jb2xvbg=="})
            void withMalformedHeader_returnsUnauthorized(String header) {
                assertGetDenied(apiWithoutAuthentication()
                        .header("Authorization", header), 401);
            }

            @Test
            void asMember_returnsForbidden() {
                // Correct credentials, but no manager permissions.
                assertGetDenied(apiAs(MEMBER_EMAIL, TEST_PASSWORD), 403);
            }
        }


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
                    expectedDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID),
                    expectedDivision(
                            "ParkShark Gent",
                            "Ghent Parking",
                            SECOND_DIRECTOR_ID
                    ),
                    expectedDivision(
                            "ParkShark Leuven",
                            "Leuven Parking",
                            DIRECTOR_ID
                    )
            );
        }

        @Test
        void afterCreatingDivision_returnsCreatedDivision() {
            // GIVEN: aanmaken via de echte POST-route.
            int createdId = api()
                    .contentType(ContentType.JSON)
                    .body(createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID))
                    .when()
                    .post("/division")
                    .then()
                    .log().ifValidationFails()
                    .statusCode(201)
                    .extract()
                    .path("id");

            // WHEN
            List<Map<String, Object>> result = getAllDivisions();

            // THEN
            assertThat(result).containsExactly(
                    expectedDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID));
            assertThat(result.getFirst().get("id")).isEqualTo(createdId);
        }
    }

    @Nested
    class GetDivision {

        private int divisionId;

        @BeforeEach
        void insertDivisionUnderTest() {
            insertDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID);
            divisionId = divisionIdByName(NAME);
        }

        @Test
        void withExistingId_returnsOkAndAllData() {
            Map<String, Object> result = getDivision(divisionId);

            // isEqualTo on the full map also proves no parentDivisionId key is present.
            assertThat(result).isEqualTo(
                    expectedDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID));
        }

        @Test
        void withSubdivision_includesParentDivisionId() {
            insertSubDivision("ParkShark Berchem", "Berchem Parking",
                    SECOND_DIRECTOR_ID, divisionId);
            int subId = divisionIdByName("ParkShark Berchem");

            Map<String, Object> expected = expectedDivision(
                    "ParkShark Berchem", "Berchem Parking", SECOND_DIRECTOR_ID);
            expected.put("parentDivisionId", divisionId);

            assertThat(getDivision(subId)).isEqualTo(expected);
        }

        @Test
        void withUnknownId_returnsNotFound() {
            api().when().get("/division/{id}", 987654)
                    .then().log().ifValidationFails()
                    .statusCode(404);
        }

        @Test
        void withNonNumericId_returnsBadRequest() {
            api().when().get("/division/{id}", "banana")
                    .then().log().ifValidationFails()
                    .statusCode(400);
        }

        @Nested
        class AccessControl {

            @Test
            void withoutCredentials_returnsUnauthorized() {
                assertGetOneDenied(apiWithoutAuthentication(), 401);
            }

            @Test
            void withWrongPassword_returnsUnauthorized() {
                assertGetOneDenied(apiAs(MANAGER_EMAIL, "incorrect-password"), 401);
            }

            @Test
            void asMember_returnsForbidden() {
                assertGetOneDenied(apiAs(MEMBER_EMAIL, TEST_PASSWORD), 403);
            }
        }

        private Map<String, Object> getDivision(int id) {
            return api().when().get("/division/{id}", id)
                    .then().log().ifValidationFails()
                    .statusCode(200)
                    .contentType(ContentType.JSON)
                    .extract().jsonPath().getMap("$");
        }

        private void assertGetOneDenied(RequestSpecification request, int expectedStatus) {
            request.when().get("/division/{id}", divisionId)
                    .then().log().ifValidationFails()
                    .statusCode(expectedStatus);
        }
    }

    @Nested
    class CreateSubdivision {

        private static final String SUB_NAME = "ParkShark Berchem";
        private static final String SUB_ORIGINAL_NAME = "Berchem Parking";

        private int parentId;

        @BeforeEach
        void insertParent() {
            insertDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID);
            parentId = divisionIdByName(NAME);
        }

        @Test
        void withExistingParent_returnsCreatedAndLinksToParent() {
            api().contentType(ContentType.JSON)
                    .body(createRequest(SUB_NAME, SUB_ORIGINAL_NAME, SECOND_DIRECTOR_ID))
                    .when().post("/division/{parentId}/subdivisions", parentId)
                    .then().log().ifValidationFails()
                    .statusCode(201)
                    .contentType(ContentType.JSON)
                    .body("name", equalTo(SUB_NAME))
                    .body("originalName", equalTo(SUB_ORIGINAL_NAME))
                    .body("directorId", equalTo(SECOND_DIRECTOR_ID))
                    .body("parentDivisionId", equalTo(parentId));

            assertThat(jdbcTemplate.queryForObject(
                    "SELECT parent_division_id FROM parkshark.division WHERE name = ?",
                    Integer.class, SUB_NAME)).isEqualTo(parentId);
        }

        @Test
        void subdivisionOfASubdivision_isAllowed() {
            insertSubDivision(SUB_NAME, SUB_ORIGINAL_NAME, DIRECTOR_ID, parentId);
            int subId = divisionIdByName(SUB_NAME);

            api().contentType(ContentType.JSON)
                    .body(createRequest("ParkShark Zurenborg", "Zurenborg Parking", DIRECTOR_ID))
                    .when().post("/division/{parentId}/subdivisions", subId)
                    .then().log().ifValidationFails()
                    .statusCode(201)
                    .body("parentDivisionId", equalTo(subId));
        }

        @Test
        void withUnknownParent_returnsNotFoundAndSavesNothing() {
            Long countBefore = countAllDivisions();

            api().contentType(ContentType.JSON)
                    .body(createRequest(SUB_NAME, SUB_ORIGINAL_NAME, DIRECTOR_ID))
                    .when().post("/division/{parentId}/subdivisions", 987654)
                    .then().log().ifValidationFails()
                    .statusCode(404);

            assertThat(countAllDivisions()).isEqualTo(countBefore);
        }

        @Test
        void withInvalidBody_returnsBadRequest() {
            Long countBefore = countAllDivisions();

            api().contentType(ContentType.JSON)
                    .body(createRequest(" ", SUB_ORIGINAL_NAME, DIRECTOR_ID))
                    .when().post("/division/{parentId}/subdivisions", parentId)
                    .then().log().ifValidationFails()
                    .statusCode(400);

            assertThat(countAllDivisions()).isEqualTo(countBefore);
        }

        @Test
        void withoutCredentials_returnsUnauthorized() {
            assertCreateSubdivisionDenied(apiWithoutAuthentication(), 401);
        }

        @Test
        void asMember_returnsForbidden() {
            assertCreateSubdivisionDenied(apiAs(MEMBER_EMAIL, TEST_PASSWORD), 403);
        }

        @Test
        void getAllDivisions_showsParentOnlyForSubdivisions() {
            insertSubDivision(SUB_NAME, SUB_ORIGINAL_NAME, DIRECTOR_ID, parentId);

            List<Map<String, Object>> result = getAllDivisions();

            Map<String, Object> expectedSub =
                    expectedDivision(SUB_NAME, SUB_ORIGINAL_NAME, DIRECTOR_ID);
            expectedSub.put("parentDivisionId", parentId);

            assertThat(result).containsExactlyInAnyOrder(
                    expectedDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID),
                    expectedSub);
        }

        private void assertCreateSubdivisionDenied(RequestSpecification request, int expectedStatus) {
            Long countBefore = countAllDivisions();
            request.contentType(ContentType.JSON)
                    .body(createRequest(SUB_NAME, SUB_ORIGINAL_NAME, DIRECTOR_ID))
                    .when().post("/division/{parentId}/subdivisions", parentId)
                    .then().log().ifValidationFails().statusCode(expectedStatus);
            assertThat(countAllDivisions()).isEqualTo(countBefore);
        }
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
                passwordEncoder.encode(TEST_PASSWORD),
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

    private void insertSubDivision(
            String name,
            String originalName,
            int directorId,
            int parentDivisionId) {

        jdbcTemplate.update("""
                INSERT INTO parkshark.division (
                    name,
                    original_name,
                    director_id,
                    parent_division_id
                )
                VALUES (?, ?, ?, ?)
                """,
                name,
                originalName,
                directorId,
                parentDivisionId
        );
    }

    private int divisionIdByName(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM parkshark.division WHERE name = ?",
                Integer.class, name);
    }

    private Map<String, Object> expectedDivision(
            String name,
            String originalName,
            int directorId) {

        // Expected response = request data + the id the database generated.
        Map<String, Object> expected = createRequest(name, originalName, directorId);
        expected.put("id", divisionIdByName(name));
        return expected;
    }

    private void insertMember() {
        // USER is the member role in the supplied migrations.
        jdbcTemplate.update("""
                INSERT INTO parkshark.postal_code (id, zip_code, label)
                VALUES (?, '9000', 'Division E2E City')
                """, MEMBER_POSTAL_CODE_ID);
        jdbcTemplate.update("""
                INSERT INTO parkshark.address (id, street, number, postal_code_id)
                VALUES (?, 'Teststraat', '1', ?)
                """, MEMBER_ADDRESS_ID, MEMBER_POSTAL_CODE_ID);
        jdbcTemplate.update("""
                INSERT INTO parkshark.license_plate (id, number, country)
                VALUES (?, 'E2E-DIV-003', 'Belgium')
                """, MEMBER_PLATE_ID);
        jdbcTemplate.update("""
                INSERT INTO parkshark.person (
                    id, first_name, last_name, email, password_hash,
                    registration_date, role, address_id, phone_number,
                    license_plate_id, membership
                ) VALUES (?, 'Arya', 'Stark', ?, ?, CURRENT_DATE,
                          'USER', ?, '+32470000003', ?, 'BRONZE')
                """, MEMBER_ID, MEMBER_EMAIL, passwordEncoder.encode(TEST_PASSWORD),
                MEMBER_ADDRESS_ID, MEMBER_PLATE_ID);
    }

    private void assertCreateDenied(RequestSpecification request, int expectedStatus) {
        Long countBefore = countAllDivisions();
        // Valid body: security is checked inside the controller, after DTO validation.
        request.contentType(ContentType.JSON)
                .body(createRequest(NAME, ORIGINAL_NAME, DIRECTOR_ID))
                .when().post("/division")
                .then().log().ifValidationFails().statusCode(expectedStatus);
        assertThat(countAllDivisions()).isEqualTo(countBefore);
    }

    private void assertGetDenied(RequestSpecification request, int expectedStatus) {
        insertDivision(NAME, ORIGINAL_NAME, DIRECTOR_ID);
        request.when().get("/division")
                .then().log().ifValidationFails().statusCode(expectedStatus);
        assertThat(countAllDivisions()).isEqualTo(1L);
    }

    private void deleteTestData() {
        // Subdivisions reference their parent: unlink first, then delete.
        jdbcTemplate.update("""
                UPDATE parkshark.division
                SET parent_division_id = NULL
                WHERE director_id IN (?, ?)
                """,
                DIRECTOR_ID,
                SECOND_DIRECTOR_ID
        );

        jdbcTemplate.update("""
                DELETE FROM parkshark.division
                WHERE director_id IN (?, ?)
                """,
                DIRECTOR_ID,
                SECOND_DIRECTOR_ID
        );

        jdbcTemplate.update("""
                DELETE FROM parkshark.person
                WHERE id IN (?, ?, ?)
                """,
                DIRECTOR_ID,
                SECOND_DIRECTOR_ID,
                MEMBER_ID
        );
        jdbcTemplate.update("DELETE FROM parkshark.address WHERE id = ?", MEMBER_ADDRESS_ID);
        jdbcTemplate.update("DELETE FROM parkshark.license_plate WHERE id = ?", MEMBER_PLATE_ID);
        jdbcTemplate.update("DELETE FROM parkshark.postal_code WHERE id = ?", MEMBER_POSTAL_CODE_ID);
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