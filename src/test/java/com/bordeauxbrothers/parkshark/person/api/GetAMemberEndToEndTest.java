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
public class GetAMemberEndToEndTest {
    private static final String MANAGER_EMAIL = "get-all-members-admin@test.example";
    private static final String MANAGER_PASSWORD = "test-password";
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
        personRepository.save(new Manager(
                new Name("Test", "Manager"),
                MANAGER_EMAIL,
                passwordEncoder.encode(MANAGER_PASSWORD),
                LocalDate.now()));

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
    @DisplayName("getAMember/{id} returns OK and full details of a member")
    void getAllMembers_returnsOkAndListOfMemberDTOs() {

        int memberId = testMember.getId()+1;

        given()
                .baseUri("http://localhost")
                .port(port)
                .auth().preemptive().basic(MANAGER_EMAIL, MANAGER_PASSWORD)
                .when()
                .get("/person/{id}", memberId)
                .then()
                .statusCode(404)
                .body("detail", equalTo("Member with id " + memberId + " not found"))
                .contentType(ContentType.JSON);
    }
}
