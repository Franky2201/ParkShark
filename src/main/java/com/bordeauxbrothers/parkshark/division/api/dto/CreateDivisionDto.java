package com.bordeauxbrothers.parkshark.division.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateDivisionDto(
        @NotBlank(message = "division name should not be blank") String name,
        @NotBlank(message = "division original name should not be blank") String originalName,
        @NotNull(message = "division should have a director") int directorId
) {
}
