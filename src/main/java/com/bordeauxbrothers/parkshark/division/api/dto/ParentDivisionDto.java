package com.bordeauxbrothers.parkshark.division.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Short reference to the parent of a subdivision.")
public record ParentDivisionDto(

        @Schema(description = "ID of the parent division.", example = "1")
        int id,

        @Schema(description = "Name of the parent division.", example = "Shark North")
        String name
) {
}