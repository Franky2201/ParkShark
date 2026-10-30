package com.bordeauxbrothers.parkshark.division.api;

import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.domain.Division;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DivisionMapper {

    public Division toEntity(CreateDivisionDto createDivisionDto) {
        return new Division(createDivisionDto.name(), createDivisionDto.originalName(), createDivisionDto.directorId());
    }

    public DivisionDto toDto(Division division) {
        return new DivisionDto(division.getName(), division.getOriginalName(), division.getDirectorId(), division.getParentDivisionId());
    }

    public List<DivisionDto> toDto(List<Division> divisionList) {
        return divisionList.stream().map(this::toDto).toList();
    }

}
