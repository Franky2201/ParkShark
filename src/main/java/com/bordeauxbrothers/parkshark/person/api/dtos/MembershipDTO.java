package com.bordeauxbrothers.parkshark.person.api.dtos;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "The membership level to switch to.")
public record MembershipDTO(

        @Schema(
                description = "New membership level. Must differ from the current level.",
                example = "GOLD",
                allowableValues = {"BRONZE", "SILVER", "GOLD"}
        )
        String membership
) {
}