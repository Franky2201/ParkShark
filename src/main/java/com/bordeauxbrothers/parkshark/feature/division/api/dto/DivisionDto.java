package com.bordeauxbrothers.parkshark.feature.division.api.dto;

import com.bordeauxbrothers.parkshark.feature.person.domain.Person;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DivisionDto(
        @NotBlank(message = "division name should not be blank") String name,
        @NotBlank(message = "division original name should not be blank") String originalName,
        @NotNull(message = "division should have a director") int directorId
) {
}
