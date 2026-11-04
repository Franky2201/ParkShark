package com.bordeauxbrothers.parkshark.parkingLot.service;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.exception.DivisionNotFoundException;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import com.bordeauxbrothers.parkshark.parkingLot.api.ParkingLotMapper;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotOverviewDto;
import com.bordeauxbrothers.parkshark.parkingLot.domain.Category;
import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import com.bordeauxbrothers.parkshark.parkingLot.exception.ContactPersonNotFoundException;
import com.bordeauxbrothers.parkshark.parkingLot.exception.InvalidContactPersonException;
import com.bordeauxbrothers.parkshark.parkingLot.exception.ParkingLotNameAlreadyExistsException;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
import com.bordeauxbrothers.parkshark.person.domain.Manager;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Name;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.service.PostalCodeService;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddressDTO;
import com.bordeauxbrothers.parkshark.person.service.dtos.PostalCodeDTO;
import com.bordeauxbrothers.parkshark.person.service.mappers.AddressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.BDDAssertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PL1 - ParkingLotService - Unit Tests")
class ParkingLotServiceTest {

    private static final int DIVISION_ID = 1;
    private static final int CONTACT_PERSON_ID = 2;

    @Mock
    private ParkingLotRepository parkingLotRepository;
    @Mock
    private DivisionRepository divisionRepository;
    @Mock
    private PersonRepository personRepository;
    @Mock
    private PostalCodeService postalCodeService;

    private ParkingLotMapper parkingLotMapper;
    private ParkingLotService parkingLotService;

    @BeforeEach
    void setUp() {
        parkingLotMapper = new ParkingLotMapper(new AddressMapper());
        parkingLotService = new ParkingLotService(
                parkingLotRepository,
                divisionRepository,
                personRepository,
                postalCodeService,
                parkingLotMapper
        );
    }

    private CreateParkingLotDto validRequest() {
        return new CreateParkingLotDto(
                "Central Station Underground",
                Category.UNDERGROUND,
                250,
                new BigDecimal("3.50"),
                new AddressDTO("CanterSteen", "12", new PostalCodeDTO("1000", "Brussels")),
                CONTACT_PERSON_ID,
                DIVISION_ID
        );
    }

    private Division division() {
        Division division = new Division("ParkShark Brussels", "Brussels Parking", 1);
        ReflectionTestUtils.setField(division, "id", DIVISION_ID);
        return division;
    }

    private Member contactPerson(String phoneNumber, String mobileNumber) {
        Member member = new Member(
                new Name("Walter", "White"),
                "walter.white@cartel.com",
                "weneedtocook",
                LocalDate.now(),
                null,
                phoneNumber,
                mobileNumber,
                null,
                null
        );
        ReflectionTestUtils.setField(member, "id", CONTACT_PERSON_ID);
        return member;
    }

    @Nested
    @DisplayName("Create a parking lot - Happy path")
    class HappyPath {

        @Test
        @DisplayName("Should save the parking lot with the requested data and return it")
        void createParkingLot_savesParkingLotWithRequestedData() {
            // Given
            Division division = division();
            Member contactPerson = contactPerson("+32475000666", null);
            when(divisionRepository.findById(DIVISION_ID)).thenReturn(Optional.of(division));
            when(personRepository.findById(CONTACT_PERSON_ID)).thenReturn(Optional.of(contactPerson));
            when(postalCodeService.findOrCreate(any(PostalCodeDTO.class)))
                    .thenReturn(new PostalCode("1000", "Brussels"));
            when(parkingLotRepository.save(any(ParkingLot.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            ParkingLotDto result = parkingLotService.createParkingLot(validRequest());

            // Then
            ArgumentCaptor<ParkingLot> captor = ArgumentCaptor.forClass(ParkingLot.class);
            verify(parkingLotRepository).save(captor.capture());
            ParkingLot saved = captor.getValue();

            assertThat(saved.getName()).isEqualTo("Central Station Underground");
            assertThat(saved.getCategory()).isEqualTo(Category.UNDERGROUND);
            assertThat(saved.getMaxCapacity()).isEqualTo(250);
            assertThat(saved.getPricePerHour()).isEqualByComparingTo("3.50");
            assertThat(saved.getContactPerson()).isSameAs(contactPerson);
            assertThat(saved.getDivision()).isSameAs(division);
            assertThat(saved.getAddress().getStreet()).isEqualTo("CanterSteen");
            assertThat(saved.getAddress().getNumber()).isEqualTo("12");
            assertThat(saved.getAddress().getPostalCode().getZipCode()).isEqualTo("1000");
            assertThat(saved.getAddress().getPostalCode().getLabel()).isEqualTo("Brussels");

            assertThat(result.name()).isEqualTo("Central Station Underground");
            assertThat(result.category()).isEqualTo(Category.UNDERGROUND);
            assertThat(result.maxCapacity()).isEqualTo(250);
            assertThat(result.pricePerHour()).isEqualByComparingTo("3.50");
            assertThat(result.contactPersonId()).isEqualTo(CONTACT_PERSON_ID);
            assertThat(result.divisionId()).isEqualTo(DIVISION_ID);
            assertThat(result.address().getStreet()).isEqualTo("CanterSteen");
            assertThat(result.address().getNumber()).isEqualTo("12");
            assertThat(result.address().getPostalCodeDTO().getZipCode()).isEqualTo("1000");
            assertThat(result.address().getPostalCodeDTO().getLabel()).isEqualTo("Brussels");
        }
    }

    @Nested
    @DisplayName("Create a parking lot - Validation and exceptions")
    class ValidationAndExceptions {

        @Test
        @DisplayName("Should throw DivisionNotFoundException and save nothing when the division does not exist")
        void createParkingLot_whenDivisionDoesNotExist_throwsDivisionNotFoundAndSavesNothing() {
            // Given
            when(divisionRepository.findById(DIVISION_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> parkingLotService.createParkingLot(validRequest()))
                    .isInstanceOf(DivisionNotFoundException.class)
                    .hasMessageContaining(String.valueOf(DIVISION_ID));

            verify(parkingLotRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ContactPersonNotFoundException and save nothing when the contact person does not exist")
        void createParkingLot_whenContactPersonDoesNotExist_throwsContactPersonNotFoundAndSavesNothing() {
            // Given
            when(divisionRepository.findById(DIVISION_ID)).thenReturn(Optional.of(division()));
            when(personRepository.findById(CONTACT_PERSON_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> parkingLotService.createParkingLot(validRequest()))
                    .isInstanceOf(ContactPersonNotFoundException.class)
                    .hasMessageContaining(String.valueOf(CONTACT_PERSON_ID));

            verify(parkingLotRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw InvalidContactPersonException and save nothing when the contact person is not a member")
        void createParkingLot_whenContactPersonIsNotAMember_throwsInvalidContactPersonAndSavesNothing() {
            // Given
            Manager manager = new Manager(new Name("Gus", "Fring"), "gus.fring@pollos.com", "hash", LocalDate.now());
            ReflectionTestUtils.setField(manager, "id", CONTACT_PERSON_ID);
            when(divisionRepository.findById(DIVISION_ID)).thenReturn(Optional.of(division()));
            when(personRepository.findById(CONTACT_PERSON_ID)).thenReturn(Optional.of(manager));

            // When & Then
            assertThatThrownBy(() -> parkingLotService.createParkingLot(validRequest()))
                    .isInstanceOf(InvalidContactPersonException.class)
                    .hasMessageContaining(String.valueOf(CONTACT_PERSON_ID));

            verify(parkingLotRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw ParkingLotNameAlreadyExistsException and save nothing when the name is already taken")
        void createParkingLot_whenNameAlreadyExists_throwsNameAlreadyExistsAndSavesNothing() {
            // Given
            CreateParkingLotDto request = validRequest();
            when(parkingLotRepository.existsByName(request.name())).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> parkingLotService.createParkingLot(request))
                    .isInstanceOf(ParkingLotNameAlreadyExistsException.class)
                    .hasMessageContaining(request.name());

            verify(parkingLotRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get all parking lots")
    class GetAllParkingLots {

        @Test
        @DisplayName("Should return all parking lots with id, name, capacity, contact email and telephone")
        void getAllParkingLots_returnsAllParkingLotsWithOverviewData() {
            // Given
            when(parkingLotRepository.findAll()).thenReturn(List.of(
                    parkingLot(100, "Central Station Underground", 250, contactPerson("+3222000111", null)),
                    parkingLot(101, "Grand Place Parking", 80, contactPerson("+3222000222", "+32475000666"))
            ));

            // When
            List<ParkingLotOverviewDto> result = parkingLotService.getAllParkingLots();

            // Then
            assertThat(result)
                    .extracting(ParkingLotOverviewDto::id,
                            ParkingLotOverviewDto::name,
                            ParkingLotOverviewDto::capacity,
                            ParkingLotOverviewDto::contactPersonEmail,
                            ParkingLotOverviewDto::contactPersonTelephone)
                    .containsExactly(
                            tuple(100, "Central Station Underground", 250, "walter.white@cartel.com", "+3222000111"),
                            tuple(101, "Grand Place Parking", 80, "walter.white@cartel.com", "+32475000666"));

            verify(parkingLotRepository).findAll();
            verifyNoInteractions(divisionRepository, personRepository, postalCodeService);
        }

        @Test
        @DisplayName("Should return an empty list when there are no parking lots")
        void getAllParkingLots_whenNoParkingLots_returnsEmptyList() {
            // Given
            when(parkingLotRepository.findAll()).thenReturn(List.of());

            // When
            List<ParkingLotOverviewDto> result = parkingLotService.getAllParkingLots();

            // Then
            assertThat(result).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("Should return a null telephone when the contact person only has a mobile number")
        void getAllParkingLots_whenContactPersonHasOnlyMobile_returnsNullTelephone() {
            // Given
            when(parkingLotRepository.findAll()).thenReturn(List.of(
                    parkingLot(100, "Central Station Underground", 250, contactPerson(null, "+32475000666"))
            ));

            // When
            List<ParkingLotOverviewDto> result = parkingLotService.getAllParkingLots();

            // Then
            assertThat(result).singleElement()
                    .extracting(ParkingLotOverviewDto::contactPersonTelephone)
                    .isEqualTo("+32475000666");
        }
    }

    /**
     * Builds a parking lot via the real mapper, with a fake DB id set by reflection.
     * Category, price, address and division are fixed defaults.
     */
    private ParkingLot parkingLot(int id, String name, int maxCapacity, Member contactPerson) {
        CreateParkingLotDto request = new CreateParkingLotDto(
                name,
                Category.UNDERGROUND,
                maxCapacity,
                new BigDecimal("3.50"),
                new AddressDTO("CanterSteen", "12", new PostalCodeDTO("1000", "Brussels")),
                CONTACT_PERSON_ID,
                DIVISION_ID
        );
        ParkingLot parkingLot = parkingLotMapper.toEntity(
                request, new PostalCode("1000", "Brussels"), contactPerson, division());
        ReflectionTestUtils.setField(parkingLot, "id", id);
        return parkingLot;
    }
}