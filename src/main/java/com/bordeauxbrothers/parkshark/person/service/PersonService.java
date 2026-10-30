package com.bordeauxbrothers.parkshark.person.service;

import com.bordeauxbrothers.parkshark.common.exception.EmailAlreadyInUseException;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.repositories.AddressRepository;
import com.bordeauxbrothers.parkshark.person.repositories.LicensePlateRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.service.mappers.PersonMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class PersonService {
    private final PersonRepository personRepository;
    private final PostalCodeRepository postalCodeRepository;
    private final AddressRepository addressRepository;
    private final LicensePlateRepository licensePlateRepository;
    private final PersonMapper personMapper;

    public PersonService(
            PersonRepository personRepository,
            PostalCodeRepository postalCodeRepository,
            AddressRepository addressRepository,
            LicensePlateRepository licensePlateRepository,
            PersonMapper personMapper
    ) {
        this.personRepository = personRepository;
        this.postalCodeRepository = postalCodeRepository;
        this.addressRepository = addressRepository;
        this.licensePlateRepository = licensePlateRepository;
        this.personMapper = personMapper;
    }

    @Transactional
    public void addMember(AddMemberDTO dto) {
        isEmailAlreadyUsed(dto.getEmail());
        var postalCodeDTO = dto.getAddressDTO().getPostalCodeDTO();

        PostalCode savedPostalCode = postalCodeRepository.save(
                new PostalCode(postalCodeDTO.getZipCode(), postalCodeDTO.getLabel())
        );

        var addressDTO = dto.getAddressDTO();
        Address savedAddress = addressRepository.save(
                new Address(
                        addressDTO.getStreet(),
                        addressDTO.getNumber(),
                        savedPostalCode
                )
        );

        var plateDTO = dto.getLicensePlateDTO();
        LicensePlate savedPlate = licensePlateRepository.save(
                new LicensePlate(plateDTO.getNumber(), plateDTO.getCountry())
        );

        Member member = personMapper.fromAddMemberDTOtoMember(
                dto,
                savedAddress,
                savedPlate
        );

        personRepository.save(member);
    }

    public void isEmailAlreadyUsed(String email){
        if (personRepository.findByEmail(email).isPresent()){
            throw new EmailAlreadyInUseException("Email already used");
        };
    }
}
