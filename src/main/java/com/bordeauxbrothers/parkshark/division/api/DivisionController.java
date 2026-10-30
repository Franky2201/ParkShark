package com.bordeauxbrothers.parkshark.division.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.service.DivisionService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.CREATE_DIVISION;
import static com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature.SEE_ALL_DIVISIONS;

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

    @GetMapping(produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    public List<DivisionDto> getAllDivisions(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        securityService.validateAuthorization(authorization, SEE_ALL_DIVISIONS);
        return divisionService.getAllDivisions();
    }

}
