package com.bordeauxbrothers.parkshark.division.api.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Response DTO for a division.
 * {@code parentDivisionId} is only present in the JSON for subdivisions.
 */
public record DivisionDto(
        int id,
        String name,
        String originalName,
        int directorId,
        @JsonInclude(JsonInclude.Include.NON_NULL) Integer parentDivisionId
) {
}
