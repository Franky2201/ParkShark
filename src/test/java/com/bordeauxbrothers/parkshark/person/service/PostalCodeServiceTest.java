package com.bordeauxbrothers.parkshark.person.service;

import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.service.dtos.PostalCodeDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PostalCodeService - Unit Tests")
class PostalCodeServiceTest {

    @Mock
    private PostalCodeRepository postalCodeRepository;

    private PostalCodeService postalCodeService;

    @BeforeEach
    void setUp() {
        postalCodeService = new PostalCodeService(postalCodeRepository);
    }

    @Nested
    @DisplayName("Find or create a postal code")
    class FindOrCreate {

        @Test
        @DisplayName("Should return the existing postal code and save nothing when zip code and label already exist")
        void findOrCreate_whenPostalCodeExists_returnsItAndSavesNothing() {
            // Given
            PostalCode existing = new PostalCode("3000", "Leuven");
            when(postalCodeRepository.findByZipCodeAndLabel("3000", "Leuven")).thenReturn(Optional.of(existing));

            // When
            PostalCode result = postalCodeService.findOrCreate(new PostalCodeDTO("3000", "Leuven"));

            // Then
            assertThat(result).isSameAs(existing);
            verify(postalCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should save and return a new postal code when zip code and label don't exist yet")
        void findOrCreate_whenPostalCodeDoesNotExist_savesAndReturnsIt() {
            // Given
            when(postalCodeRepository.findByZipCodeAndLabel("3000", "Leuven")).thenReturn(Optional.empty());
            when(postalCodeRepository.save(any(PostalCode.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            PostalCode result = postalCodeService.findOrCreate(new PostalCodeDTO("3000", "Leuven"));

            // Then
            ArgumentCaptor<PostalCode> captor = ArgumentCaptor.forClass(PostalCode.class);
            verify(postalCodeRepository).save(captor.capture());
            assertThat(captor.getValue().getZipCode()).isEqualTo("3000");
            assertThat(captor.getValue().getLabel()).isEqualTo("Leuven");
            assertThat(result).isSameAs(captor.getValue());
        }
    }
}
