package com.bordeauxbrothers.parkshark.division.service;

import com.bordeauxbrothers.parkshark.division.api.DivisionMapper;
import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.domain.DivisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DivisionServiceTest {

    @Mock
    private DivisionRepository divisionRepository;

    private DivisionService divisionService;

    @BeforeEach
    void setUp() {
        divisionService = new DivisionService(
                divisionRepository,
                new DivisionMapper()
        );
    }

    @Test
    void createDivision_savesDivisionWithRequestedData() {
        // GIVEN
        CreateDivisionDto request = new CreateDivisionDto(
                "The North",
                "Winterfell Parking",
                1
        );

        when(divisionRepository.save(any(Division.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // WHEN
        DivisionDto result = divisionService.createDivision(request);

        // THEN: juiste gegevens doorgegeven aan de repository
        ArgumentCaptor<Division> captor =
                ArgumentCaptor.forClass(Division.class);

        verify(divisionRepository).save(captor.capture());

        Division savedDivision = captor.getValue();

        assertThat(savedDivision.getName()).isEqualTo("The North");
        assertThat(savedDivision.getOriginalName())
                .isEqualTo("Winterfell Parking");
        assertThat(savedDivision.getDirectorId()).isEqualTo(1);

        // THEN: juiste gegevens teruggegeven aan de aanroeper
        assertThat(result.name()).isEqualTo("The North");
        assertThat(result.originalName()).isEqualTo("Winterfell Parking");
        assertThat(result.directorId()).isEqualTo(1);
    }

    @Test
    void createDivision_whenSavingFails_propagatesException() {
        // GIVEN
        CreateDivisionDto request = new CreateDivisionDto(
                "The North",
                "Winterfell Parking",
                1
        );

        DataIntegrityViolationException exception =
                new DataIntegrityViolationException(
                        "Database rejected the division"
                );

        when(divisionRepository.save(any(Division.class)))
                .thenThrow(exception);

        // WHEN + THEN
        assertThatThrownBy(() -> divisionService.createDivision(request))
                .isSameAs(exception);
    }
}