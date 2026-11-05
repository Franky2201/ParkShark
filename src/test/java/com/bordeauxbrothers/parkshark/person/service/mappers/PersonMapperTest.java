package com.bordeauxbrothers.parkshark.person.service.mappers;

import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddressDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.LicensePlateDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.PostalCodeDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PersonMapperTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PersonMapper personMapper;

    @Test
    void fromAddMemberDTOtoMember() {
        AddMemberDTO dto = aValidAddMemberDTO();
        Member source = aValidMember();
        when(passwordEncoder.encode(dto.password())).thenReturn("encoded-password");

        Member result = personMapper.fromAddMemberDTOtoMember(
                dto, source.getAddress(), source.getLicensePlate());

        assertEquals(dto.name().getFirstName(), result.getName().getFirstName());
        assertEquals(dto.name().getLastName(), result.getName().getLastName());
        assertEquals(dto.email(), result.getEmail());
        assertEquals("encoded-password", result.getPasswordHash());
        assertEquals(dto.phoneNumber(), result.getPhoneNumber());
        assertEquals(dto.mobileNumber(), result.getMobileNumber());
        assertSame(source.getAddress(), result.getAddress());
        assertSame(source.getLicensePlate(), result.getLicensePlate());
        assertEquals(dto.membership(), result.getMembership());
        assertEquals(LocalDate.now(), result.getRegistrationDate());
    }

    @Test
    void fromMemberToGetAllMembersDTO() {
        Member member = aValidMember();

        var result = personMapper.fromMemberToGetAllMembersDTO(member);

        assertNull(result.id());
        assertEquals(member.getName().getFirstName(), result.name().getFirstName());
        assertEquals(member.getName().getLastName(), result.name().getLastName());
        assertEquals(member.getLicensePlate().getNumber(), result.licensePlateNumber());
        assertEquals(member.getPhoneNumber(), result.phoneNumber());
        assertEquals(member.getEmail(), result.email());
        assertEquals(member.getRegistrationDate(), result.registrationDate());
    }

    private AddMemberDTO aValidAddMemberDTO() {
        return new AddMemberDTO(
                new Name("Ciccio", "Test"),
                new AddressDTO(
                        "Test Street",
                        "42",
                        new PostalCodeDTO("1000", "Brussels")
                ),
                "+3221234567",
                "+32470123456",
                "ciccio.test@example.com",
                "password123",
                new LicensePlateDTO("1-ABC-123", "Belgium"),
                Membership.BRONZE
        );
    }

    private Member aValidMember() {
        PostalCode postalCode = new PostalCode("1000", "Brussels");
        Address address = new Address("Test Street", "42", postalCode);
        LicensePlate licensePlate = new LicensePlate("1-ABC-123", "Belgium");

        return new Member(
                new Name("Ciccio", "Test"),
                "ciccio.test@example.com",
                "encoded-password",
                LocalDate.of(2025, 1, 15),
                address,
                "+3221234567",
                "+32470123456",
                licensePlate,
                Membership.BRONZE
        );
    }
}
