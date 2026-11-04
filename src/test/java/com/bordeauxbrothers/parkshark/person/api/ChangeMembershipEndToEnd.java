package com.bordeauxbrothers.parkshark.person.api;

import com.bordeauxbrothers.parkshark.person.domain.Manager;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.*;
import com.bordeauxbrothers.parkshark.person.repositories.AddressRepository;
import com.bordeauxbrothers.parkshark.person.repositories.LicensePlateRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.service.dtos.MembershipDTO;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.web.servlet.function.RequestPredicates.contentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class ChangeMembershipEndToEnd {
    private static final String MEMBER_EMAIL = "get-all-members@test.example";
    private static final String MEMBER_PASSWORD = "member-password";
    private static Member testMember;

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
        PostalCode postalCode = postalCodeRepository.save(
                new PostalCode("9000", "Test City"));
        Address address = addressRepository.save(
                new Address("Test Street", "1", postalCode));
        LicensePlate licensePlate = licensePlateRepository.save(
                new LicensePlate("TEST-001", "Belgium"));

        testMember = personRepository.save(new Member(
                new Name("Test", "Member"),
                MEMBER_EMAIL,
                passwordEncoder.encode(MEMBER_PASSWORD),
                LocalDate.now(),
                address,
                "+32123456789",
                null,
                licensePlate,
                null));
    }

    @AfterEach
    void cleanUp() {
        personRepository.findByEmail(MEMBER_EMAIL).ifPresent(personRepository::delete);
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
    @DisplayName("ChangeMembership returns Ok and changes membership")
    void changeMembership_returnsOkAndChangesMembershipStatus() {

        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MEMBER_EMAIL, MEMBER_PASSWORD)
                .contentType(ContentType.JSON)
                .body("{\"membership\":\"SILVER\"}")
                .when()
                .post("/person/membership")
                .then()
                .statusCode(200);

        Member updatedMember = (Member) personRepository.findByEmail(MEMBER_EMAIL)
                .orElseThrow();

        Assertions.assertEquals(Membership.SILVER, updatedMember.getMembership());
    }

    @Test
    @DisplayName("ChangeMembership throws UnchangedMembership Exception when an already existing Membership is selected")
    void changeMembership_returnsUnchangedMembershipExceptionWhenAlreadyExistingMembershipIsSelected() {

        MembershipDTO dto = new MembershipDTO("BRONZE");
        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MEMBER_EMAIL, MEMBER_PASSWORD)
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/person/membership")
                .then()
                .statusCode(409)
                .body("detail", equalTo(String.format(
                        "Membership hasn't changed. Your membership was already: %s.",
                        Membership.BRONZE.getLabel()
                ))).contentType(ContentType.JSON);
    }

    @Test
    @DisplayName("ChangeMembership throws UnchangedMembership Exception when an already existing Membership is selected")
    void changeMembership_returnsInvalidMembershipExceptionWhenInvalidMembershipIsSelected() {

        MembershipDTO dto = new MembershipDTO("RUBY");
        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MEMBER_EMAIL, MEMBER_PASSWORD)
                .contentType(ContentType.JSON)
                .body(dto)
                .when()
                .post("/person/membership")
                .then()
                .statusCode(400)
                .body("detail", equalTo(String.format("Membership %s is not valid", dto.getMembership().trim().toUpperCase())
                )).contentType(ContentType.JSON);
    }
}
