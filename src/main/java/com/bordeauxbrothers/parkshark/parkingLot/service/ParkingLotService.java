package com.bordeauxbrothers.parkshark.parkingLot.service;

import com.bordeauxbrothers.parkshark.division.domain.Division;
import com.bordeauxbrothers.parkshark.division.exception.DivisionNotFoundException;
import com.bordeauxbrothers.parkshark.division.repository.DivisionRepository;
import com.bordeauxbrothers.parkshark.parkingLot.api.ParkingLotMapper;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.CreateParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.api.dto.ParkingLotDto;
import com.bordeauxbrothers.parkshark.parkingLot.exception.ContactPersonNotFoundException;
import com.bordeauxbrothers.parkshark.parkingLot.exception.InvalidContactPersonException;
import com.bordeauxbrothers.parkshark.parkingLot.exception.ParkingLotNameAlreadyExistsException;
import com.bordeauxbrothers.parkshark.parkingLot.repository.ParkingLotRepository;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.service.PostalCodeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ParkingLotService {

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
        if (parkingLotRepository.existsByName(createParkingLotDto.name())) {
            throw new ParkingLotNameAlreadyExistsException(createParkingLotDto.name());
        }
        Division division = divisionRepository.findById(createParkingLotDto.divisionId())
                .orElseThrow(() -> new DivisionNotFoundException(createParkingLotDto.divisionId()));
        Member contactPerson = findContactPerson(createParkingLotDto.contactPersonId());
        PostalCode postalCode = postalCodeService.findOrCreate(createParkingLotDto.address().getPostalCodeDTO());

        return parkingLotMapper.toDto(
                parkingLotRepository.save(
                        parkingLotMapper.toEntity(createParkingLotDto, postalCode, contactPerson, division)));
    }

    private Member findContactPerson(int contactPersonId) {
        Person person = personRepository.findById(contactPersonId)
                .orElseThrow(() -> new ContactPersonNotFoundException(contactPersonId));
        if (!(person instanceof Member member)) {
            throw new InvalidContactPersonException(contactPersonId, "only members can be a contact person");
        }
        return member;
    }
}
