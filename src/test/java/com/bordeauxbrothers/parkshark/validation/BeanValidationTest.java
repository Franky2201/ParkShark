package com.bordeauxbrothers.parkshark.validation;

import com.bordeauxbrothers.parkshark.allocation.domain.ParkingSpotAllocation;
import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddressDTO;
import com.bordeauxbrothers.parkshark.person.service.dtos.LicensePlateDTO;
import com.bordeauxbrothers.parkshark.person.service.dtos.PostalCodeDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class BeanValidationTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        validatorFactory.close();
    }

    @Test
    void addMember_rejectsMissingRequiredAndMalformedValuesIncludingNestedDtos() {
        AddMemberDTO dto = new AddMemberDTO(
                new Name(" ", " "),
                new AddressDTO(
                        " ",
                        " ",
                        new PostalCodeDTO(" ", " ")
                ),
                " ",
                null,
                "not-an-email",
                "123",
                new LicensePlateDTO(" ", " "),
                null
        );

        Set<String> violations = propertyPaths(validator.validate(dto));

        assertThat(violations).contains(
                "name.firstName",
                "name.lastName",
                "addressDTO.street",
                "addressDTO.number",
                "addressDTO.postalCodeDTO.zipCode",
                "addressDTO.postalCodeDTO.label",
                "licensePlateDTO.number",
                "licensePlateDTO.country",
                "email",
                "password",
                "addMemberDTO"
        );
    }

    @Test
    void addMember_acceptsEitherPhoneNumberAndValidInput() {
        AddMemberDTO dto = new AddMemberDTO(
                new Name("Ciccio", "Test"),
                new AddressDTO("Test Street", "1",
                        new PostalCodeDTO("1000", "Brussels")),
                null,
                "+32470123456",
                "ciccio.test@example.com",
                "password123",
                new LicensePlateDTO("1-ABC-123", "Belgium"),
                null
        );

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void addMember_acceptsPhoneNumberWhenMobileIsMissing() {
        AddMemberDTO dto = validAddMember(
                "+3221234567", null, "ciccio.test@example.com", "password123");

        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void addMember_rejectsWhenBothPhoneNumbersAreBlank() {
        AddMemberDTO dto = validAddMember(" ", "", "ciccio.test@example.com", "password123");

        assertThat(propertyPaths(validator.validate(dto))).contains("addMemberDTO");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "invalid", "person+tag@example.com"})
    void addMember_rejectsEmailOutsideItsAllowedFormat(String email) {
        AddMemberDTO dto = validAddMember("+3221234567", null, email, "password123");

        assertThat(propertyPaths(validator.validate(dto))).contains("email");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "     ", "12345"})
    void addMember_rejectsBlankOrShortPassword(String password) {
        AddMemberDTO dto = validAddMember(
                "+3221234567", null, "ciccio.test@example.com", password);

        assertThat(propertyPaths(validator.validate(dto))).contains("password");
    }

    @Test
    void createDivision_rejectsBlankNames() {
        CreateDivisionDto dto = new CreateDivisionDto(" ", "", 1);

        assertThat(propertyPaths(validator.validate(dto)))
                .contains("name", "originalName");
    }

    @Test
    void division_rejectsBlankNames() {
        Division division = new Division(" ", "", 1);

        assertThat(propertyPaths(validator.validate(division)))
                .contains("name", "originalName");
    }

    @Test
    void parkingSpotAllocation_rejectsNonPositiveIdsAndInvalidPlate() {
        ParkingSpotAllocation allocation = new ParkingSpotAllocation(
                0,
                -1,
                " ".repeat(31),
                OffsetDateTime.now()
        );

        assertThat(propertyPaths(validator.validate(allocation)))
                .contains("memberId", "parkingLotId", "licensePlateNumber");
    }

    private static Set<String> propertyPaths(
            Set<? extends jakarta.validation.ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(violation -> violation.getPropertyPath().toString().isEmpty()
                        ? "addMemberDTO"
                        : violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }

    private static AddMemberDTO validAddMember(
            String phoneNumber,
            String mobileNumber,
            String email,
            String password) {
        return new AddMemberDTO(
                new Name("Ciccio", "Test"),
                new AddressDTO("Test Street", "1",
                        new PostalCodeDTO("1000", "Brussels")),
                phoneNumber,
                mobileNumber,
                email,
                password,
                new LicensePlateDTO("1-ABC-123", "Belgium"),
                null
        );
    }
}
