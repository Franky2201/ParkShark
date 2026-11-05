package com.bordeauxbrothers.parkshark.person.service;

import com.bordeauxbrothers.parkshark.person.api.dtos.MemberDTO;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import com.bordeauxbrothers.parkshark.person.exception.EmailAlreadyInUseException;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.PostalCode;
import com.bordeauxbrothers.parkshark.person.exception.InvalidMembershipException;
import com.bordeauxbrothers.parkshark.person.exception.NotExistingUserException;
import com.bordeauxbrothers.parkshark.person.repositories.AddressRepository;
import com.bordeauxbrothers.parkshark.person.repositories.LicensePlateRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import com.bordeauxbrothers.parkshark.person.repositories.PostalCodeRepository;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.GetAllMembersDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.MembershipDTO;
import com.bordeauxbrothers.parkshark.person.service.mappers.PersonMapper;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class PersonService {
    private final Logger log = LoggerFactory.getLogger(PersonService.class);

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

    public void addMember(AddMemberDTO dto) {
        isEmailAlreadyUsed(dto.getEmail());
        var postalCodeDTO = dto.getAddressDTO().getPostalCodeDTO();

        log.info("Saving postal code {}", postalCodeDTO);
        PostalCode savedPostalCode = postalCodeRepository.save(
                new PostalCode(postalCodeDTO.getZipCode(), postalCodeDTO.getLabel())
        );

        log.info("Saving address {}", dto.getAddressDTO());
        var addressDTO = dto.getAddressDTO();
        Address savedAddress = addressRepository.save(
                new Address(
                        addressDTO.getStreet(),
                        addressDTO.getNumber(),
                        savedPostalCode
                )
        );

        var plateDTO = dto.getLicensePlateDTO();

        log.info("Saving license plate {}", plateDTO);
        LicensePlate savedPlate = licensePlateRepository.save(
                new LicensePlate(plateDTO.getNumber(), plateDTO.getCountry())
        );

        Member member = personMapper.fromAddMemberDTOtoMember(
                dto,
                savedAddress,
                savedPlate
        );

        log.info("Saving member {}", member);
        personRepository.save(member);
    }

    public void isEmailAlreadyUsed(String email) {
        log.info("Checking if email {} is already used", email);
        if (personRepository.findByEmail(email).isPresent()) {
            throw new EmailAlreadyInUseException("Email already used");
        }
        ;
    }

    @Transactional(readOnly = true)
    public List<GetAllMembersDTO> getAllMembers() {
        return personRepository.getAllMembers().stream()
                .map(personMapper::fromMemberToGetAllMembersDTO)
                .toList();
    }

    public void changeMembership(MembershipDTO membership, Person user) {
        Membership requested;
        try {
            requested = Membership.valueOf(membership.getMembership().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            log.error("Membership {} is not valid", membership.getMembership().trim().toUpperCase());
            throw new InvalidMembershipException(
                    String.format("Membership %s is not valid", membership.getMembership().trim().toUpperCase())
            );
        }
        log.info("Casting Person to Member");
        Member member = (Member) user;
        member.setMembership(requested);
        personRepository.save(member);
    }

    @Transactional(readOnly = true)
    public MemberDTO getMember(int id) {
        log.info("Getting member with id {}", id);
        return personRepository.findMemberById(id)
                .map(personMapper::toMemberDTO)
                .orElseThrow(() -> new NotExistingUserException(id));
    }
}
