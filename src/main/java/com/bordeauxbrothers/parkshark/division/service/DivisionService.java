package com.bordeauxbrothers.parkshark.division.service;

import com.bordeauxbrothers.parkshark.division.api.dto.CreateDivisionDto;
import com.bordeauxbrothers.parkshark.division.api.dto.DivisionDto;
import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.exception.DivisionNotFoundException;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DivisionService {

    private final DivisionRepository divisionRepository;
    private final DivisionMapper divisionMapper;
    Logger log = LoggerFactory.getLogger(DivisionService.class);

    public DivisionService(DivisionRepository divisionRepository, DivisionMapper divisionMapper) {
        this.divisionRepository = divisionRepository;
        this.divisionMapper = divisionMapper;
    }

    public DivisionDto createDivision(CreateDivisionDto createDivisionDto) {
        log.debug("Creating division");
        return divisionMapper.toDto(
                divisionRepository.save(
                        divisionMapper.toEntity(createDivisionDto)));
    }

    public DivisionDto createSubDivision(int parentDivisionId, @Valid CreateDivisionDto createDivisionDto) {
        log.debug("Creating subdivision under division {}", parentDivisionId);
        // Look up the parent (or fail with 404)
        Division parent = divisionRepository.findById(parentDivisionId)
                .orElseThrow(() -> new DivisionNotFoundException(parentDivisionId));

        Division subDivision = divisionMapper.toEntity(createDivisionDto);
        parent.addSubdivision(subDivision);

        return divisionMapper.toDto(divisionRepository.save(subDivision));
    }

    @Transactional(readOnly = true)
    public List<DivisionDto> getAllDivisions() {
        log.debug("Getting all divisions");
        return divisionMapper.toDto(
                divisionRepository.findAllBy());
    }

    @Transactional(readOnly = true)
    public DivisionDto getDivision(int id) {
        log.debug("Getting division {}", id);
        return divisionRepository.findById(id)
                .map(divisionMapper::toDto)
                .orElseThrow(() -> new DivisionNotFoundException(id));
    }
}
