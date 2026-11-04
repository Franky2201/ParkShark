package com.bordeauxbrothers.parkshark.allocation.api;

import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.domain.AllocationStatus;
import com.bordeauxbrothers.parkshark.allocation.service.ParkingSpotAllocationService;
import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.*;

@Tag(
        name = "Parking spot allocations",
        description = "Start and manage parking sessions for members."
)
@RestController
@RequestMapping(path = "allocation")
public class ParkingSpotAllocationController {

    private final ParkingSpotAllocationService parkingSpotAllocationService;
    private final SecurityService securityService;

    public ParkingSpotAllocationController(ParkingSpotAllocationService parkingSpotAllocationService, SecurityService securityService) {
        this.parkingSpotAllocationService = parkingSpotAllocationService;
        this.securityService = securityService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Start a parking spot allocation",
            description = """
                Starts a parking session for the authenticated member.

                The member is identified through Basic Authentication;
                no member ID is required in the request body.

                Requirements:
                - The parking lot must exist and have available capacity.
                - Bronze and Silver members must use their registered licence plate number.
                - Gold members may use a different licence plate number.

                The server generates the allocation ID and start time.
                The new allocation has status ACTIVE and no end time.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "The actual car being parked and the destination parking lot.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = StartAllocationDto.class),
                            examples = @ExampleObject(
                                    name = "Start a parking session",
                                    value = """
                                        {
                                          "licencePlate": {
                                            "number": "1-ABC-123",
                                            "country": "BE"
                                          },
                                          "parkingLotId": 42
                                        }
                                        """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Allocation started successfully.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AllocationDto.class),
                    examples = @ExampleObject(
                            value = """
                                {
                                  "id": 101,
                                  "memberId": 7,
                                  "parkingLotId": 42,
                                  "licensePlateNumber": "1-ABC-123",
                                  "startTime": "2026-11-04T10:00:00Z",
                                  "endTime": null,
                                  "status": "ACTIVE"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request, or the licence plate does not match a Bronze/Silver member.",
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
            description = "The authenticated user is not allowed to start an allocation.",
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
    @ApiResponse(
            responseCode = "409",
            description = "The parking lot is full.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public AllocationDto startAllocatingParkingSpot(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody StartAllocationDto startAllocationDto) {
        Person user = securityService.validateAuthorization(authorization, CREATE_PARKING_LOT_ALLOCATION);
        return parkingSpotAllocationService.startAllocatingParkingSpot(startAllocationDto, user);
    }

    @PutMapping(path = "/stop/{id}", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    public AllocationDto stopAllocatingParkingSpot(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable int id) {
        Person user = securityService.validateAuthorization(authorization, STOP_PARKING_LOT_ALLOCATION);
        return parkingSpotAllocationService.stopAllocatingParkingSpot(id, user);
    }

    @GetMapping( produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    public List<AllocationDto> getAllAllocatingParkingSpotsFiltered(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(name = "limit", required = false) Integer limit,
            @RequestParam(name = "status", required = false) AllocationStatus status,
            @RequestParam(name = "desc", required = false) boolean desc) {
        securityService.validateAuthorization(authorization, SEE_ALL_ALLOCATIONS);
        return parkingSpotAllocationService.getAllAllocatingParkingSpotsFiltered(limit, status, desc);
    }


}
