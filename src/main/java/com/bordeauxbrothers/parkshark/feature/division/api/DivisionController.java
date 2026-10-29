package com.bordeauxbrothers.parkshark.feature.division.api;

import com.bordeauxbrothers.parkshark.feature.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.feature.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.feature.division.service.DivisionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/division")
public class DivisionController {

    private final DivisionService divisionService;

    public DivisionController(DivisionService divisionService) {
        this.divisionService = divisionService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    public DivisionDto createDivision(
            @Valid @RequestBody CreateDivisionDto createDivisionDto) {
        return divisionService.createDivision(createDivisionDto);
    }

}
