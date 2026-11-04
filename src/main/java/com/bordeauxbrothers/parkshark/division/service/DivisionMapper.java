package com.bordeauxbrothers.parkshark.division.service;

import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.ParentDivisionDto;
import com.bordeauxbrothers.parkshark.division.domain.Division;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DivisionMapper {

    public Division toEntity(CreateDivisionDto createDivisionDto) {
        return new Division(createDivisionDto.name(), createDivisionDto.originalName(), createDivisionDto.directorId());
    }

    public DivisionDto toDto(Division division) {
        Division parent = division.getParentDivision();
        ParentDivisionDto parentDivisionDto = parent == null
                ? null
                : new ParentDivisionDto(parent.getId(), parent.getName());

        return new DivisionDto(
                division.getId(),
                division.getName(),
                division.getOriginalName(),
                division.getDirectorId(), // TODO: refactor to name
                parentDivisionDto);
    }

    public List<DivisionDto> toDto(List<Division> divisionList) {
        return divisionList.stream().map(this::toDto).toList();
    }

}
