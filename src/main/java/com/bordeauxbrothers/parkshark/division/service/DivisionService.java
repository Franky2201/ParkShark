package com.bordeauxbrothers.parkshark.division.service;

import com.bordeauxbrothers.parkshark.common.exception.ResourceNotFoundException;
import com.bordeauxbrothers.parkshark.division.api.DivisionMapper;
import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import jakarta.validation.Valid;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DivisionService {

    private final DivisionRepository divisionRepository;
    private final DivisionMapper divisionMapper;

    public DivisionService(DivisionRepository divisionRepository, DivisionMapper divisionMapper) {
        this.divisionRepository = divisionRepository;
        this.divisionMapper = divisionMapper;
    }

    public DivisionDto createDivision(CreateDivisionDto createDivisionDto) {
        return divisionMapper.toDto(
                divisionRepository.save(
                        divisionMapper.toEntity(createDivisionDto)));
    }

    public DivisionDto createSubDivision(int parentDivisionId, @Valid CreateDivisionDto createDivisionDto) {
        // Look up the parent (or fail with 404)
        Division parent = divisionRepository.findById(parentDivisionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Parent division with id " + parentDivisionId + " does not exist"));

        Division subDivision = divisionMapper.toEntity(createDivisionDto);
        parent.addSubdivision(subDivision);

        return divisionMapper.toDto(divisionRepository.save(subDivision));
    }

    @Transactional(readOnly = true)
    public List<DivisionDto> getAllDivisions() {
        return divisionMapper.toDto(
                divisionRepository.findAll());
    }
}
