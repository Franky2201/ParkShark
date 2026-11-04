package com.bordeauxbrothers.parkshark.parkingLot.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.service.ParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.CREATE_PARKING_LOT;

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
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateParkingLotDto createParkingLotDto) {
        securityService.validateAuthorization(authorization, CREATE_PARKING_LOT);
        return parkingLotService.createParkingLot(createParkingLotDto);
    }
}
