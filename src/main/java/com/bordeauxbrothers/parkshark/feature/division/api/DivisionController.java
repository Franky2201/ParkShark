package com.bordeauxbrothers.parkshark.feature.division.api;

import com.bordeauxbrothers.parkshark.feature.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.feature.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.feature.division.service.DivisionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/division")
public class DivisionController {

    private final DivisionService divisionService;

    public DivisionController(DivisionService divisionService) {
        this.divisionService = divisionService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public DivisionDto createDivision(
            @Valid @RequestBody CreateDivisionDto createDivisionDto) {
        return divisionService.createDivision(createDivisionDto);
    }

}
