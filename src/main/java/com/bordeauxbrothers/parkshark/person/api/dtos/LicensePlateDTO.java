package com.bordeauxbrothers.parkshark.person.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "A licence plate: number and issuing country.")
public record LicensePlateDTO(

        @Schema(description = "Licence plate number.", example = "1-ABC-123")
        @NotBlank(message = "License plate number should not be blank")
        String number,

        @Schema(description = "Country that issued the licence plate.", example = "BE")
        @NotBlank(message = "License plate country should not be blank")
        String country
) {
}