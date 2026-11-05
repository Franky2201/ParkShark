package com.bordeauxbrothers.parkshark.person.service;

import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.exception.EmailAlreadyInUseException;
import com.bordeauxbrothers.parkshark.person.repositories.AddressRepository;
import com.bordeauxbrothers.parkshark.person.repositories.LicensePlateRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddressDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.GetAllMembersDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.LicensePlateDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.PostalCodeDTO;
import com.bordeauxbrothers.parkshark.person.service.mappers.PersonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonServiceTest {

    @Mock
    private PersonRepository personRepository;
    @Mock
    private PostalCodeRepository postalCodeRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private LicensePlateRepository licensePlateRepository;
    @Mock
    private PersonMapper personMapper;

    @InjectMocks
    private PersonService personService;

    private AddMemberDTO addMemberDTO;

    @BeforeEach
    void setUp() {
        addMemberDTO = validAddMemberDTO();
    }

    @Test
    void addMember_savesRelatedEntitiesAndMappedMember() {
        PostalCode postalCode = new PostalCode("1000", "Brussels");
        Address address = new Address("Test Street", "42", postalCode);
        LicensePlate licensePlate = new LicensePlate("1-ABC-123", "Belgium");
        Member member = mock(Member.class);

        when(personRepository.findByEmail(addMemberDTO.email()))
                .thenReturn(Optional.empty());
        when(postalCodeRepository.save(any(PostalCode.class)))
                .thenReturn(postalCode);
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(licensePlateRepository.save(any(LicensePlate.class)))
                .thenReturn(licensePlate);
        when(personMapper.fromAddMemberDTOtoMember(addMemberDTO, address, licensePlate))
                .thenReturn(member);

        personService.addMember(addMemberDTO);

        ArgumentCaptor<PostalCode> postalCodeCaptor =
                ArgumentCaptor.forClass(PostalCode.class);
        ArgumentCaptor<Address> addressCaptor = ArgumentCaptor.forClass(Address.class);
        ArgumentCaptor<LicensePlate> licensePlateCaptor =
                ArgumentCaptor.forClass(LicensePlate.class);

        verify(postalCodeRepository).save(postalCodeCaptor.capture());
        verify(addressRepository).save(addressCaptor.capture());
        verify(licensePlateRepository).save(licensePlateCaptor.capture());
        verify(personMapper).fromAddMemberDTOtoMember(addMemberDTO, address, licensePlate);
        verify(personRepository).save(member);

        assertThat(postalCodeCaptor.getValue().getZipCode()).isEqualTo("1000");
        assertThat(postalCodeCaptor.getValue().getLabel()).isEqualTo("Brussels");
        assertThat(addressCaptor.getValue().getStreet()).isEqualTo("Test Street");
        assertThat(addressCaptor.getValue().getNumber()).isEqualTo("42");
        assertThat(addressCaptor.getValue().getPostalCode()).isSameAs(postalCode);
        assertThat(licensePlateCaptor.getValue().getNumber()).isEqualTo("1-ABC-123");
        assertThat(licensePlateCaptor.getValue().getCountry()).isEqualTo("Belgium");
    }

    @Test
    void addMember_whenEmailAlreadyExists_throwsAndSavesNothing() {
        when(personRepository.findByEmail(addMemberDTO.email()))
                .thenReturn(Optional.of(mock(Person.class)));

        assertThatThrownBy(() -> personService.addMember(addMemberDTO))
                .isInstanceOf(EmailAlreadyInUseException.class)
                .hasMessage("Email already used");

        verifyNoInteractions(postalCodeRepository, addressRepository,
                licensePlateRepository, personMapper);
        verify(personRepository, never()).save(any());
    }

    @Test
    void getAllMembers_returnsMappedMembers() {
        Member firstMember = mock(Member.class);
        Member secondMember = mock(Member.class);
        GetAllMembersDTO firstDto = new GetAllMembersDTO(
                1, new Name("First", "Member"), "1-AAA-111", "+32111111111",
                "first@example.com", LocalDate.of(2025, 1, 1));
        GetAllMembersDTO secondDto = new GetAllMembersDTO(
                2, new Name("Second", "Member"), "1-BBB-222", "+32222222222",
                "second@example.com", LocalDate.of(2025, 1, 2));
        when(personRepository.getAllMembers()).thenReturn(List.of(firstMember, secondMember));
        when(personMapper.fromMemberToGetAllMembersDTO(firstMember)).thenReturn(firstDto);
        when(personMapper.fromMemberToGetAllMembersDTO(secondMember)).thenReturn(secondDto);

        List<GetAllMembersDTO> result = personService.getAllMembers();

        assertThat(result).containsExactly(firstDto, secondDto);
        verify(personMapper).fromMemberToGetAllMembersDTO(firstMember);
        verify(personMapper).fromMemberToGetAllMembersDTO(secondMember);
    }

    @Test
    void getAllMembers_whenRepositoryIsEmpty_returnsEmptyList() {
        when(personRepository.getAllMembers()).thenReturn(List.of());

        assertThat(personService.getAllMembers()).isEmpty();

        verifyNoInteractions(personMapper);
    }

    private AddMemberDTO validAddMemberDTO() {
        return new AddMemberDTO(
                new Name("Test", "Member"),
                new AddressDTO("Test Street", "42",
                        new PostalCodeDTO("1000", "Brussels")),
                "+3221234567",
                "+32470123456",
                "test.member@example.com",
                "password123",
                new LicensePlateDTO("1-ABC-123", "Belgium"),
                Membership.BRONZE
        );
    }
}
