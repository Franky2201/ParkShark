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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.*;

@RestController
@RequestMapping(path = "/parking-lots")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;
    private final SecurityService securityService;

    public ParkingLotController(ParkingLotService parkingLotService, SecurityService securityService) {
        this.parkingLotService = parkingLotService;
        this.securityService = securityService;
    }

    @Operation(
            summary = "Create a parking lot",
            description = """
                    Creates a new parking lot in an existing division (or subdivision).
                    The contact person must be an existing member. Requires the MANAGER role.
                    """)
    @ApiResponse(responseCode = "201", description = "Parking lot created")
    @ApiResponse(responseCode = "400", description = "Invalid request body, or the contact person is not a member",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid credentials",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Authenticated, but not allowed to create parking lots",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Division or contact person not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A parking lot with this name already exists",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    public ParkingLotDto createParkingLot(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateParkingLotDto createParkingLotDto) {
        securityService.validateAuthorization(authorization, CREATE_PARKING_LOT);
        return parkingLotService.createParkingLot(createParkingLotDto);
    }

    @Operation(
            summary = "Get all parking lots",
            description = """
                    Returns an overview of all parking lots: id, name, capacity,
                    and the contact person's email and telephone. Requires the MANAGER role.
                    """)
    @ApiResponse(responseCode = "200", description = "List of parking lots (empty if none exist)",
            content = @Content(mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = ParkingLotOverviewDto.class))))
    @ApiResponse(responseCode = "401", description = "Missing or invalid credentials",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Authenticated, but not allowed to see all parking lots",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirement(name = "basicAuth")
    public List<ParkingLotOverviewDto> getAllParkingLots(
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        securityService.validateAuthorization(authorization, SEE_ALL_PARKING_LOTS);
        return parkingLotService.getAllParkingLots();
    }

    @Operation(
            summary = "Get a specific parking lot",
            description = """
                    Returns all data of a single parking lot: id, name, category, capacity, price per hour,
                    address, the full contact person (name, mobile phone, telephone, email, address)
                    and its division. Requires the MANAGER role.
                    """)
    @ApiResponse(responseCode = "200", description = "Single parking lot",
            content = @Content(mediaType = "application/json", schema = @Schema(implementation = ParkingLotOverviewDto.class)))
    @ApiResponse(responseCode = "401", description = "Missing or invalid credentials",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "403", description = "Authenticated, but not allowed to see parking lot details",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No parking lot exists with the given id",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping(path = "/{id}", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirement(name = "basicAuth")
    public ParkingLotDto getParkingLot(
            @Parameter(description = "Id of the parking lot", example = "1") @PathVariable int id,
            @Parameter(hidden = true) @RequestHeader(value = "Authorization", required = false) String authorization) {
        securityService.validateAuthorization(authorization, SEE_SPECIFIC_PARKING_LOT);
        return parkingLotService.getParkingLot(id);
    }
}
