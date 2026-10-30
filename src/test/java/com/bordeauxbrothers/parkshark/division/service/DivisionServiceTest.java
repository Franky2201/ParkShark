package com.bordeauxbrothers.parkshark.division.service;

import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;
import com.bordeauxbrothers.parkshark.division.api.DivisionMapper;
import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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

    @Nested
    @DisplayName("Create a subdivision")
    class CreateSubDivision {

        @Test
        @DisplayName("Given an existing parent id when both relationships are correctly filled in")
        void createSubdivision_linksNewDivisionToExistingParent() {
            // GIVEN
            Division parent = new Division("The North", "Winterfell Parking", 1);
            ReflectionTestUtils.setField(parent, "id", 7);
            when(divisionRepository.findById(7)).thenReturn(Optional.of(parent));
            when(divisionRepository.save(any(Division.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            CreateDivisionDto request = new CreateDivisionDto("The Wall", "Castle Black Parking", 2);

            // WHEN
            DivisionDto result = divisionService.createSubDivision(7, request);

            // THEN
            ArgumentCaptor<Division> captor = ArgumentCaptor.forClass(Division.class);
            verify(divisionRepository).save(captor.capture());
            Division saved = captor.getValue();

            // Check parent
            assertThat(saved.getParentDivisionId()).isEqualTo(7);
            assertThat(parent.getSubdivisions()).containsExactly(saved);

            // Check child
            assertThat(result.name()).isEqualTo("The Wall");
            assertThat(result.originalName()).isEqualTo("Castle Black Parking");
            assertThat(result.directorId()).isEqualTo(2);
            assertThat(result.parentDivisionId()).isEqualTo(7);
        }

        @Test
        @DisplayName("When parent does not exist throws Not Found and saves nothing")
        void createSubdivision_whenParentDoesNotExist_throwsNotFoundAndSavesNothing() {
            // GIVEN
            when(divisionRepository.findById(42)).thenReturn(Optional.empty());
            CreateDivisionDto request = new CreateDivisionDto("The Wall", "Castle Black Parking", 2);

            // WHEN + THEN
            assertThatThrownBy(() -> divisionService.createSubDivision(42, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("42");

            verify(divisionRepository, never()).save(any());
        }

        @Test
        @DisplayName("When division has no parent")
        void createDivision_hasNoParent() {
            // GIVEN
            when(divisionRepository.save(any(Division.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // WHEN
            DivisionDto result = divisionService.createDivision(new CreateDivisionDto("The North", "Winterfell Parking", 1));

            // THEN
            assertThat(result.parentDivisionId()).isNull();
        }
    }

}