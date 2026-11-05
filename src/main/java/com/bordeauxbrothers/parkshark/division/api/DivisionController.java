package com.bordeauxbrothers.parkshark.division.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.service.DivisionService;
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
        name = "Divisions",
        description = "Create and review divisions and subdivisions (stories DI1–DI4)."
)
@RestController
@RequestMapping(path = "/division")
public class DivisionController {

    private final DivisionService divisionService;
    private final SecurityService securityService;
    Logger log = LoggerFactory.getLogger(DivisionController.class);

    public DivisionController(DivisionService divisionService, SecurityService securityService) {
        this.divisionService = divisionService;
        this.securityService = securityService;
    }

    // ----------------------------------------------------------------- DI1

    @PostMapping(consumes = "application/json", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a division",
            description = """
                Story DI1. Creates a new top-level division, intended for managers.

                A division represents a company taken over by ParkShark.

                Requirements:
                - The name and original name must not be blank.

                The server generates the division ID.
                A top-level division has no parent division.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "The current name, original company name and director of the division.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateDivisionDto.class),
                            examples = @ExampleObject(
                                    name = "Create a division",
                                    value = """
                                        {
                                          "name": "Shark North",
                                          "originalName": "Parkings Dupont NV",
                                          "directorId": 3
                                        }
                                        """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Division created successfully.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DivisionDto.class),
                    examples = @ExampleObject(
                            value = """
                                {
                                  "id": 1,
                                  "name": "Shark North",
                                  "originalName": "Parkings Dupont NV",
                                  "directorId": 3
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request (e.g. blank name or original name, missing director).",
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
            description = "The authenticated user is not allowed to create divisions.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public DivisionDto createDivision(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateDivisionDto createDivisionDto) {
        log.info("Trying to create division {}", createDivisionDto);
        securityService.validateAuthorization(authorization, CREATE_DIVISION);
        return divisionService.createDivision(createDivisionDto);
    }

    // ----------------------------------------------------------------- DI4

    @PostMapping(path = "/{parentDivisionId}/subdivisions", consumes = "application/json", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a subdivision",
            description = """
                Story DI4. Creates a new division as a subdivision of an existing division, intended for managers.

                A subdivision represents a company taken over by a company that ParkShark took over.

                Requirements:
                - The parent division must exist.
                - The name and original name must not be blank.
                - The director must be an existing person.

                The server generates the division ID.
                The response includes the parent division.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "The current name, original company name and director of the subdivision.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CreateDivisionDto.class),
                            examples = @ExampleObject(
                                    name = "Create a subdivision",
                                    value = """
                                        {
                                          "name": "Shark North - Antwerp",
                                          "originalName": "Garage Peeters BV",
                                          "directorId": 5
                                        }
                                        """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Subdivision created successfully.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DivisionDto.class),
                    examples = @ExampleObject(
                            value = """
                                {
                                  "id": 2,
                                  "name": "Shark North - Antwerp",
                                  "originalName": "Garage Peeters BV",
                                  "directorId": 5,
                                  "parentDivision": {
                                    "id": 1,
                                    "name": "Shark North"
                                  }
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request (e.g. blank name or original name, missing director).",
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
            description = "The authenticated user is not allowed to create subdivisions.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The parent division does not exist.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public DivisionDto createSubDivision(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "ID of the existing parent division.", example = "1")
            @PathVariable int parentDivisionId,
            @Valid @RequestBody CreateDivisionDto createDivisionDto) {
        log.info("Trying to create subdivision {} under division {}", createDivisionDto, parentDivisionId);
        securityService.validateAuthorization(authorization, CREATE_SUB_DIVISION);
        return divisionService.createSubDivision(parentDivisionId, createDivisionDto);
    }

    // ----------------------------------------------------------------- DI2

    @GetMapping(produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get all divisions",
            description = """
                Story DI2. Returns all divisions and subdivisions, intended for managers.

                Subdivisions include their parent division;
                top-level divisions have no parentDivision field.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Divisions retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = DivisionDto.class)),
                    examples = @ExampleObject(
                            value = """
                                [
                                  {
                                    "id": 1,
                                    "name": "Shark North",
                                    "originalName": "Parkings Dupont NV",
                                    "directorId": 3
                                  },
                                  {
                                    "id": 2,
                                    "name": "Shark North - Antwerp",
                                    "originalName": "Garage Peeters BV",
                                    "directorId": 5,
                                    "parentDivision": {
                                      "id": 1,
                                      "name": "Shark North"
                                    }
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
            description = "The authenticated user is not allowed to see all divisions.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public List<DivisionDto> getAllDivisions(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Trying to get all divisions");
        securityService.validateAuthorization(authorization, SEE_ALL_DIVISIONS);
        return divisionService.getAllDivisions();
    }

    // ----------------------------------------------------------------- DI3

    @GetMapping(path = "/{id}", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get a division",
            description = """
                Story DI3. Returns a single division, intended for managers.

                If the division is a subdivision, its parent division is included.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Division retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = DivisionDto.class)
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
            description = "The authenticated user is not allowed to see divisions.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The division does not exist.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public DivisionDto getDivision(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "ID of the division.", example = "1")
            @PathVariable int id) {
        log.info("Trying to get division with id {}", id);
        securityService.validateAuthorization(authorization, SEE_SPECIFIC_DIVISION);
        return divisionService.getDivision(id);
    }
}
