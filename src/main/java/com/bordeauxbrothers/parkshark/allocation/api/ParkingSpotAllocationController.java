package com.bordeauxbrothers.parkshark.allocation.api;

import com.bordeauxbrothers.parkshark.allocation.api.dto.AllocationDto;
import com.bordeauxbrothers.parkshark.allocation.api.dto.StartAllocationDto;
import com.bordeauxbrothers.parkshark.allocation.service.ParkingSpotAllocationService;
import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.CREATE_PARKING_LOT_ALLOCATION;

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
    public AllocationDto startAllocatingParkingSpot(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody StartAllocationDto startAllocationDto) {
        Person user = securityService.validateAuthorization(authorization, CREATE_PARKING_LOT_ALLOCATION);
        return parkingSpotAllocationService.startAllocatingParkingSpot(startAllocationDto, user);
    }

}
