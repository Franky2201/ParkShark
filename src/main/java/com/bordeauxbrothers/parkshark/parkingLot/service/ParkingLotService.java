package com.bordeauxbrothers.parkshark.parkingLot.service;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.exception.DivisionNotFoundException;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import com.bordeauxbrothers.parkshark.parkingLot.api.ParkingLotMapper;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotOverviewDto;
import com.bordeauxbrothers.parkshark.parkingLot.domain.ParkingLot;
import com.bordeauxbrothers.parkshark.parkingLot.exception.ContactPersonNotFoundException;
import com.bordeauxbrothers.parkshark.parkingLot.exception.InvalidContactPersonException;
import com.bordeauxbrothers.parkshark.parkingLot.exception.ParkingLotNameAlreadyExistsException;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.service.PostalCodeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ParkingLotService {

    private final Logger log = LoggerFactory.getLogger(ParkingLotService.class);

    private final ParkingLotRepository parkingLotRepository;
    private final DivisionRepository divisionRepository;
    private final PersonRepository personRepository;
    private final PostalCodeService postalCodeService;
    private final ParkingLotMapper parkingLotMapper;

    public ParkingLotService(ParkingLotRepository parkingLotRepository,
                             DivisionRepository divisionRepository,
                             PersonRepository personRepository,
                             PostalCodeService postalCodeService,
                             ParkingLotMapper parkingLotMapper) {
        this.parkingLotRepository = parkingLotRepository;
        this.divisionRepository = divisionRepository;
        this.personRepository = personRepository;
        this.postalCodeService = postalCodeService;
        this.parkingLotMapper = parkingLotMapper;
    }

    public ParkingLotDto createParkingLot(CreateParkingLotDto createParkingLotDto) {
        log.debug("Creating parking lots '{}' for division {}", createParkingLotDto.name(), createParkingLotDto.divisionId());

        if (parkingLotRepository.existsByName(createParkingLotDto.name())) {
            throw new ParkingLotNameAlreadyExistsException(createParkingLotDto.name());
        }
        Division division = divisionRepository.findById(createParkingLotDto.divisionId())
                .orElseThrow(() -> new DivisionNotFoundException(createParkingLotDto.divisionId()));
        Member contactPerson = findContactPerson(createParkingLotDto.contactPersonId());
        PostalCode postalCode = postalCodeService.findOrCreate(createParkingLotDto.address().getPostalCodeDTO());

        ParkingLot saved = parkingLotRepository.save(
                parkingLotMapper.toEntity(createParkingLotDto, postalCode, contactPerson, division));

        log.info("Parking lot {} ('{}') created in division {}", saved.getId(), saved.getName(), division.getId());
        return parkingLotMapper.toDto(saved);
    }

    private Member findContactPerson(int contactPersonId) {
        Person person = personRepository.findById(contactPersonId)
                .orElseThrow(() -> new ContactPersonNotFoundException(contactPersonId));
        if (!(person instanceof Member member)) {
            throw new InvalidContactPersonException(contactPersonId, "only members can be a contact person");
        }
        return member;
    }

    @Transactional(readOnly = true)
    public List<ParkingLotOverviewDto> getAllParkingLots() {
        List<ParkingLotOverviewDto> lots = parkingLotRepository.findAll().stream()
                .map(parkingLotMapper::toOverviewDto)
                .toList();
        log.debug("Retrieved {} parking lots", lots.size());
        return lots;
    }
}
