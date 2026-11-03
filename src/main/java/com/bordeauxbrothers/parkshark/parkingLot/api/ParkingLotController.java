package com.bordeauxbrothers.parkshark.parkingLot.api;

import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.service.ParkingLotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/parking-lots")
public class ParkingLotController {

    private final ParkingLotService parkingLotService;

    public ParkingLotController(ParkingLotService parkingLotService) {
        this.parkingLotService = parkingLotService;
    }

    @Operation(
            summary = "Create a parking lot",
            description = """
                    Creates a new parking lot in an existing division (or subdivision).
                    The contact person must be an existing member.
                    """)
    @ApiResponse(responseCode = "201", description = "Parking lot created")
    @ApiResponse(responseCode = "400", description = "Invalid request body, or the contact person is not a member",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Division or contact person not found",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "A parking lot with this name already exists",
            content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public ParkingLotDto createParkingLot(@Valid @RequestBody CreateParkingLotDto createParkingLotDto) {
        return parkingLotService.createParkingLot(createParkingLotDto);
    }
}
