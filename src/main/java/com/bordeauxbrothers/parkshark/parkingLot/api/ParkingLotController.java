package com.bordeauxbrothers.parkshark.parkingLot.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotOverviewDto;
import com.bordeauxbrothers.parkshark.parkingLot.service.ParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.*;

@Tag(
        name = "Parking lots",
        description = "Create and review parking lots (stories PL1–PL3, DIPL1)."
)
@RestController
@RequestMapping(path = "/parking-lots")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final SecurityService securityService;
    Logger log = LoggerFactory.getLogger(ParkingLotController.class);

    public ParkingLotController(ParkingLotService parkingLotService, SecurityService securityService) {
        this.parkingLotService = parkingLotService;
        this.securityService = securityService;
    }

    // ----------------------------------------------------------- PL1 + DIPL1

    @PostMapping(consumes = "application/json", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a parking lot",
            description = """
                Stories PL1 and DIPL1. Creates a new parking lot, intended for managers.

                Requirements:
                - The parking lot name must be unique.
                - The parking lot must belong to an existing division or subdivision.
                - The contact person must be an existing member.
                - The max capacity and price per hour must be greater than zero.
                - The price per hour has at most 2 decimals.

                The server generates the parking lot ID.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "The details, address, contact person and division of the parking lot.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateParkingLotDto.class),
                            examples = @ExampleObject(
                                    name = "Create a parking lot",
                                    value = """
                                        {
                                          "name": "Shark Central Underground",
                                          "category": "UNDERGROUND",
                                          "maxCapacity": 250,
                                          "pricePerHour": 2.50,
                                          "address": {
                                            "street": "Bondgenotenlaan",
                                            "number": "12",
                                            "postal_code": {
                                              "zip_code": "3000",
                                              "label": "Leuven"
                                            }
                                          },
                                          "contactPersonId": 7,
                                          "divisionId": 1
                                        }
                                        """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Parking lot created successfully.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ParkingLotDto.class),
                    examples = @ExampleObject(
                            value = """
                                {
                                  "id": 42,
                                  "name": "Shark Central Underground",
                                  "category": "UNDERGROUND",
                                  "maxCapacity": 250,
                                  "pricePerHour": 2.50,
                                  "contactPersonId": 7,
                                  "division": {
                                    "id": 1,
                                    "name": "Shark North",
                                    "originalName": "Parkings Dupont NV",
                                    "directorId": 3
                                  },
                                  "address": {
                                    "street": "Bondgenotenlaan",
                                    "number": "12",
                                    "postal_code": {
                                      "zip_code": "3000",
                                      "label": "Leuven"
                                    }
                                  }
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request (e.g. blank name, missing address, non-positive capacity or price), or the contact person is not a member.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is missing or invalid.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The authenticated user is not allowed to create parking lots.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The division or the contact person does not exist.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "A parking lot with this name already exists.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ParkingLotDto createParkingLot(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateParkingLotDto createParkingLotDto) {
        log.info("Trying to create parking lot.");
        securityService.validateAuthorization(authorization, CREATE_PARKING_LOT);
        return parkingLotService.createParkingLot(createParkingLotDto);
    }

    // ----------------------------------------------------------------- PL2

    @GetMapping(produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get all parking lots",
            description = """
                Story PL2. Returns an overview of all parking lots, intended for managers.

                Each parking lot only includes its ID, name, capacity,
                and the contact person's email and telephone number.
                Returns an empty list if no parking lots exist.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Parking lots retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ParkingLotOverviewDto.class)),
                    examples = @ExampleObject(
                            value = """
                                [
                                  {
                                    "id": 42,
                                    "name": "Shark Central Underground",
                                    "capacity": 250,
                                    "contactPersonEmail": "tony@parkshark.be",
                                    "contactPersonTelephone": "+32 16 12 34 56"
                                  }
                                ]
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is missing or invalid.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The authenticated user is not allowed to see all parking lots.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public List<ParkingLotOverviewDto> getAllParkingLots(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Trying to get all parking lots.");
        securityService.validateAuthorization(authorization, SEE_ALL_PARKING_LOTS);
        return parkingLotService.getAllParkingLots();
    }

    // ----------------------------------------------------------- PL3 + DIPL1

    @GetMapping(path = "/{id}", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get a parking lot",
            description = """
                Stories PL3 and DIPL1. Returns a single parking lot, intended for managers.

                Includes the details, address, contact person ID
                and the full division the parking lot belongs to.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Parking lot retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ParkingLotDto.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is missing or invalid.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The authenticated user is not allowed to see parking lots.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The parking lot does not exist.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public ParkingLotDto getParkingLot(
            @Parameter(description = "Id of the parking lot", example = "42") @PathVariable int id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Trying to get parking lot with id {}", id);
        securityService.validateAuthorization(authorization, SEE_SPECIFIC_PARKING_LOT);
        return parkingLotService.getParkingLot(id);
    }
}
