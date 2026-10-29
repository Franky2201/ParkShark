package com.bordeauxbrothers.parkshark.feature.division.api;

import com.bordeauxbrothers.parkshark.feature.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.feature.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.feature.division.domain.Division;
import org.springframework.stereotype.Component;

@Component
public class DivisionMapper {

    public Division toEntity(CreateDivisionDto createDivisionDto) {
        return new Division(createDivisionDto.name(), createDivisionDto.originalName(), createDivisionDto.director());
    }

    public DivisionDto toDto(Division division) {
        return new DivisionDto(division.getName(), division.getOriginalName(), division.getDirector());
    }

}
