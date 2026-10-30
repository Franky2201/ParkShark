package com.bordeauxbrothers.parkshark.division.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.service.DivisionService;
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

import java.util.List;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.*;

@RestController
@RequestMapping(path = "/division")
public class DivisionController {

    private final DivisionService divisionService;
    private final SecurityService securityService;

    public DivisionController(DivisionService divisionService, SecurityService securityService) {
        this.divisionService = divisionService;
        this.securityService = securityService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    public DivisionDto createDivision(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateDivisionDto createDivisionDto) {
        securityService.validateAuthorization(authorization, CREATE_DIVISION);
        return divisionService.createDivision(createDivisionDto);
    }

    @Operation(summary = "Create a subdivision", description = "Sometimes, a company that was bought by ParkShark, bought some companies itself. We want to be able to express that in our application by creating subdivisions.")
    @ApiResponse(responseCode = "201", description = "Subdivision created")
    @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "No division found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping(path = "/{parentDivisionId}/subdivisions", consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirement(name = "basicAuth")
    public DivisionDto createSubDivision(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CreateDivisionDto createDivisionDto,
            @PathVariable int parentDivisionId ) {
        securityService.validateAuthorization(authorization, CREATE_SUB_DIVISION);
        return divisionService.createSubDivision(parentDivisionId, createDivisionDto);
    }

    @GetMapping(produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    public List<DivisionDto> getAllDivisions(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        securityService.validateAuthorization(authorization, SEE_ALL_DIVISIONS);
        return divisionService.getAllDivisions();
    }
}
