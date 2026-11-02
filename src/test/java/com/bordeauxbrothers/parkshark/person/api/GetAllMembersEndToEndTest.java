package com.bordeauxbrothers.parkshark.person.api;

import com.bordeauxbrothers.parkshark.person.domain.Manager;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.AddressRepository;
import com.bordeauxbrothers.parkshark.person.repositories.LicensePlateRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GetAllMembersEndToEndTest {

    private static final String MANAGER_EMAIL = "get-all-members-admin@test.example";
    private static final String MANAGER_PASSWORD = "test-password";
    private static final String MEMBER_EMAIL = "get-all-members@test.example";
    private static final String MEMBER_PASSWORD = "member-password";

    @LocalServerPort
    private int port;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private PostalCodeRepository postalCodeRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private LicensePlateRepository licensePlateRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        personRepository.save(new Manager(
                new Name("Test", "Manager"),
                MANAGER_EMAIL,
                passwordEncoder.encode(MANAGER_PASSWORD),
                LocalDate.now()));
    }

    @AfterEach
    void cleanUp() {
        personRepository.findByEmail(MEMBER_EMAIL).ifPresent(personRepository::delete);
        personRepository.findByEmail(MANAGER_EMAIL).ifPresent(personRepository::delete);
        licensePlateRepository.findAll().stream()
                .filter(plate -> "TEST-001".equals(plate.getNumber()))
                .forEach(licensePlateRepository::delete);
        addressRepository.findAll().stream()
                .filter(address -> "Test Street".equals(address.getStreet()))
                .forEach(address -> {
                    PostalCode postalCode = address.getPostalCode();
                    addressRepository.delete(address);
                    postalCodeRepository.delete(postalCode);
                });
    }

    @Test
    @DisplayName("getAllMembers returns OK and a list of all members")
    void getAllMembers_returnsOkAndListOfMemberDTOs() {
        PostalCode postalCode = postalCodeRepository.save(
                new PostalCode("9000", "Test City"));
        Address address = addressRepository.save(
                new Address("Test Street", "1", postalCode));
        LicensePlate licensePlate = licensePlateRepository.save(
                new LicensePlate("TEST-001", "Belgium"));

        personRepository.save(new Member(
                new Name("Test", "Member"),
                MEMBER_EMAIL,
                passwordEncoder.encode("member-password"),
                LocalDate.now(),
                address,
                "+32123456789",
                null,
                licensePlate,
                null));

        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MANAGER_EMAIL, MANAGER_PASSWORD)
                .accept(ContentType.JSON)
                .when()
                .get("/person/members")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(1))
                .body("[0].id", notNullValue())
                .body("[0].name.firstName", equalTo("Test"))
                .body("[0].name.lastName", equalTo("Member"))
                .body("[0].licensePlateNumber", equalTo("TEST-001"))
                .body("[0].phoneNumber", equalTo("+32123456789"))
                .body("[0].email", equalTo(MEMBER_EMAIL))
                .body("[0].registrationDate", notNullValue());
    }

    @Test
    @DisplayName("getAllMembers returns 403 Forbidden if the authUser is not a manager")
    void getAllMembers_returnsForbiddenIfNotAManager() {
        PostalCode postalCode = postalCodeRepository.save(
                new PostalCode("9000", "Test City"));
        Address address = addressRepository.save(
                new Address("Test Street", "1", postalCode));
        LicensePlate licensePlate = licensePlateRepository.save(
                new LicensePlate("TEST-001", "Belgium"));

        personRepository.save(new Member(
                new Name("Test", "Member"),
                MEMBER_EMAIL,
                passwordEncoder.encode("member-password"),
                LocalDate.now(),
                address,
                "+32123456789",
                null,
                licensePlate,
                null));
        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MEMBER_EMAIL, MEMBER_PASSWORD)
                .accept(ContentType.JSON)
                .when()
                .get("/person/members")
                .then()
                .statusCode(403)
                .body("detail", equalTo("You don't have the rights to access this resource."))
                .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("getAllMembers returns 401 Unauthorized if the credentials are invalid")
    void getAllMembers_returnsUnauthorizedWithWrongCredentials(){
        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MEMBER_EMAIL, MANAGER_PASSWORD)
                .accept(ContentType.JSON)
                .when()
                .get("/person/members")
                .then()
                .statusCode(401)
                .body("detail", equalTo("Invalid credentials"))
                .contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("getAllMembers returns an empty list if there is no member in the database")
    void getAllMembers_returnsEmptyListIfNoMemberInDatabase() {
        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MANAGER_EMAIL, MANAGER_PASSWORD)
                .accept(ContentType.JSON)
                .when()
                .get("/person/members")
                .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("$", hasSize(0));
    }
}
