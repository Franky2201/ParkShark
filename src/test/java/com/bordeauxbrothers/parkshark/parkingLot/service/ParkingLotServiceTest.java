package com.bordeauxbrothers.parkshark.parkingLot.service;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import com.bordeauxbrothers.parkshark.parkingLot.api.ParkingLotMapper;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.domain.Category;
import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    private ParkingLotService parkingLotService;

    @BeforeEach
    void setUp() {
        parkingLotService = new ParkingLotService(
                parkingLotRepository,
                divisionRepository,
                personRepository,
                postalCodeService,
                new ParkingLotMapper(new AddressMapper())
        );
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
}