package com.bordeauxbrothers.parkshark.allocation.api;

import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.domain.AllocationStatus;
import com.bordeauxbrothers.parkshark.allocation.service.ParkingSpotAllocationService;
import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.person.domain.Person;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.*;

@Tag(
        name = "Parking spot allocations",
        description = "Start, stop and review parking sessions (stories PSA1–PSA6)."
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

    // ---------------------------------------------------------------- PSA1

    @PostMapping(consumes = "application/json", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Start a parking spot allocation",
            description = """
                Story PSA1. Starts a parking session for the authenticated member.

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
                                          "licensePlate": {
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
            responseCode = "201",
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
                                  "duration": "00:00:00",
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

    // ---------------------------------------------------------------- PSA2

    @PutMapping(path = "/stop/{id}", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Stop a parking spot allocation",
            description = """
                Story PSA2. Stops an active parking session of the authenticated member.

                The member is identified through Basic Authentication;
                no member ID is required in the request.

                Requirements:
                - The allocation must exist.
                - The allocation must belong to the authenticated member.
                - The allocation must be ACTIVE.

                The server sets the end time to now.
                The allocation status changes from ACTIVE to STOPPED.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Allocation stopped successfully.",
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
                                  "endTime": "2026-11-04T12:20:15Z",
                                  "duration": "02:20:15",
                                  "status": "STOPPED"
                                }
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
            description = "The authenticated user is not allowed to stop allocations, or does not own this allocation.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The allocation does not exist.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The allocation is not ACTIVE (already stopped or invoiced).",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public AllocationDto stopAllocatingParkingSpot(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "ID of the allocation to stop.", example = "101")
            @PathVariable int id) {
        Person user = securityService.validateAuthorization(authorization, STOP_PARKING_LOT_ALLOCATION);
        return parkingSpotAllocationService.stopAllocatingParkingSpot(id, user);
    }

    // ------------------------------------------------------- PSA3 + PSA6

    @GetMapping(produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get all parking spot allocations",
            description = """
                Stories PSA3 and PSA6. Returns all allocations, intended for managers.

                By default, all allocations are returned, ordered by start time ascending.

                Optional filters:
                - limit: return at most this number of allocations.
                - status: return only allocations with this status.
                - desc: order by start time descending instead.

                Each allocation includes its duration in hh:mm:ss.
                For ACTIVE allocations, the duration is measured until now.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Allocations retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AllocationDto.class)),
                    examples = @ExampleObject(
                            value = """
                                [
                                  {
                                    "id": 101,
                                    "memberId": 7,
                                    "parkingLotId": 42,
                                    "licensePlateNumber": "1-ABC-123",
                                    "startTime": "2026-11-04T10:00:00Z",
                                    "endTime": "2026-11-04T12:20:15Z",
                                    "duration": "02:20:15",
                                    "status": "STOPPED"
                                  },
                                  {
                                    "id": 102,
                                    "memberId": 9,
                                    "parkingLotId": 42,
                                    "licensePlateNumber": "2-XYZ-987",
                                    "startTime": "2026-11-04T11:15:00Z",
                                    "endTime": null,
                                    "duration": "01:05:42",
                                    "status": "ACTIVE"
                                  }
                                ]
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid filter value (e.g. unknown status or non-numeric limit).",
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
            description = "The authenticated user is not allowed to see all allocations.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public List<AllocationDto> getAllAllocatingParkingSpotsFiltered(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "Maximum number of allocations to return. Omit to return all.", example = "100")
            @RequestParam(name = "limit", required = false) Integer limit,
            @Parameter(description = "Only return allocations with this status. Omit to return all.")
            @RequestParam(name = "status", required = false) AllocationStatus status,
            @Parameter(description = "Order by start time descending instead of ascending.")
            @RequestParam(name = "desc", defaultValue = "false") boolean desc) {
        securityService.validateAuthorization(authorization, SEE_ALL_ALLOCATIONS);
        return parkingSpotAllocationService.getAllAllocatingParkingSpotsFiltered(limit, status, desc);
    }

    // ---------------------------------------------------------------- PSA4

    @GetMapping(path = "/member/{id}", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get all parking spot allocations of a member",
            description = """
                Story PSA4. Returns all allocations of a given member, intended for managers.

                Optional filter:
                - status: return only allocations with this status.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Allocations of the member retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AllocationDto.class))
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid filter value (e.g. unknown status).",
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
            description = "The authenticated user is not allowed to see allocations of members.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The member does not exist.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public List<AllocationDto> getAllAllocatingParkingSpotsForMemberFiltered(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "ID of the member.", example = "7")
            @PathVariable int id,
            @Parameter(description = "Only return allocations with this status. Omit to return all.")
            @RequestParam(name = "status", required = false) AllocationStatus status) {
        securityService.validateAuthorization(authorization, SEE_MEMBER_PARKINGLOT_ALLOCATIONS);
        return parkingSpotAllocationService.getAllAllocatingParkingSpotsForMemberFiltered(id, status);
    }

    // ---------------------------------------------------------------- PSA5

    @GetMapping(path = "/parkinglot/{id}", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get all parking spot allocations of a parking lot",
            description = """
                Story PSA5. Returns all allocations in a given parking lot, intended for managers.

                Optional filter:
                - status: return only allocations with this status.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Allocations of the parking lot retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = AllocationDto.class))
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid filter value (e.g. unknown status).",
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
            description = "The authenticated user is not allowed to see allocations of parking lots.",
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
    public List<AllocationDto> getAllAllocatingParkingSpotsForParkingLotFiltered(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "ID of the parking lot.", example = "42")
            @PathVariable int id,
            @Parameter(description = "Only return allocations with this status. Omit to return all.")
            @RequestParam(name = "status", required = false) AllocationStatus status) {
        securityService.validateAuthorization(authorization, SEE_ALLOCATIONS_FOR_SPECIFIC_PARKING_LOT);
        return parkingSpotAllocationService.getAllAllocatingParkingSpotsForParkingLotFiltered(id, status);
    }
}