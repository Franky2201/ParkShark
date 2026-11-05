package com.bordeauxbrothers.parkshark.division.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Information required to create a division or subdivision.")
public record CreateDivisionDto(

        @Schema(description = "Current name of the division within ParkShark.", example = "Shark North")
        @NotBlank(message = "division name should not be blank")
        String name,

        @Schema(description = "Name of the company before it was taken over.", example = "Parkings Dupont NV")
        @NotBlank(message = "division original name should not be blank")
        String originalName,

        @Schema(description = "ID of the person who directs the division.", example = "3")
        @NotNull(message = "division should have a director")
        @Positive(message = "Director ID must be greater than zero")
        Integer directorId
) {
}