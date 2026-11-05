package com.bordeauxbrothers.parkshark.person.service.mappers;

import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.service.dtos.GetAllMembersDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PersonMapper {
    PasswordEncoder passwordEncoder;
    Logger log = LoggerFactory.getLogger(PersonMapper.class);

    public PersonMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Member fromAddMemberDTOtoMember(
            AddMemberDTO dto,
            Address address,
            LicensePlate licensePlate
    ) {
        log.debug("Mapping AddMemberDTO to Member");
        return new Member(
                dto.getName(),
                dto.getEmail(),
                passwordEncoder.encode(dto.getPassword()),
                LocalDate.now(),
                address,
                dto.getPhoneNumber(),
                dto.getMobileNumber(),
                licensePlate,
                dto.getMembership()
        );
    }

    public GetAllMembersDTO fromMemberToGetAllMembersDTO(Member member){
        log.debug("Mapping Member to GetAllMembersDTO");
        return new GetAllMembersDTO(
                member.getId(),
                member.getName(),
                member.getLicensePlate().getNumber(),
                getOnePhoneNumber(member.getPhoneNumber(), member.getMobileNumber()),
                member.getEmail(),
                member.getRegistrationDate()
        );
    }

    private String hashedPassword(String password){
        log.debug("Hashing password");
        return passwordEncoder.encode(password);
    }

    private String getOnePhoneNumber(String phoneNumber, String mobileNumber){
        return phoneNumber != null ? phoneNumber : mobileNumber;
    }
}
