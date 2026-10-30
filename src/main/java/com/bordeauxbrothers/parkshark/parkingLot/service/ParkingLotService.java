package com.bordeauxbrothers.parkshark.parkingLot.service;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import com.bordeauxbrothers.parkshark.parkingLot.api.ParkingLotMapper;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.service.dtos.PostalCodeDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParkingLotService {

    private final ParkingLotRepository parkingLotRepository;
    private final DivisionRepository divisionRepository;
    private final PersonRepository personRepository;
    private final PostalCodeRepository postalCodeRepository;
    private final ParkingLotMapper parkingLotMapper;

    public ParkingLotService(ParkingLotRepository parkingLotRepository,
                             DivisionRepository divisionRepository,
                             PersonRepository personRepository,
                             PostalCodeRepository postalCodeRepository,
                             ParkingLotMapper parkingLotMapper) {
        this.parkingLotRepository = parkingLotRepository;
        this.divisionRepository = divisionRepository;
        this.personRepository = personRepository;
        this.postalCodeRepository = postalCodeRepository;
        this.parkingLotMapper = parkingLotMapper;
    }

    public ParkingLotDto createParkingLot(CreateParkingLotDto createParkingLotDto) {
        Division division = divisionRepository.findById(createParkingLotDto.divisionId()).orElseThrow();
        Member contactPerson = (Member) personRepository.findById(createParkingLotDto.contactPersonId()).orElseThrow();
        PostalCode postalCode = findOrCreatePostalCode(createParkingLotDto.address().getPostalCodeDTO());

        return parkingLotMapper.toDto(
                parkingLotRepository.save(
                        parkingLotMapper.toEntity(createParkingLotDto, postalCode, contactPerson, division)));
    }

    private PostalCode findOrCreatePostalCode(PostalCodeDTO postalCodeDTO) {
        return postalCodeRepository.findByZipCodeAndLabel(postalCodeDTO.getZipCode(), postalCodeDTO.getLabel())
                .orElseGet(() -> postalCodeRepository.save(
                        new PostalCode(postalCodeDTO.getZipCode(), postalCodeDTO.getLabel())));
    }
}
