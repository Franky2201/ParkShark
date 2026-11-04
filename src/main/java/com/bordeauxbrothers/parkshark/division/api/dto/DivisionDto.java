package com.bordeauxbrothers.parkshark.division.api.dto;


import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Response DTO for a division.
 * {@code ParentDivsionDto} is only present in the JSON for subdivisions.
 */
public record DivisionDto(
        int id,
        String name,
        String originalName,
        int directorId,
        @JsonInclude(JsonInclude.Include.NON_NULL) ParentDivisionDto parentDivision
) {
}
