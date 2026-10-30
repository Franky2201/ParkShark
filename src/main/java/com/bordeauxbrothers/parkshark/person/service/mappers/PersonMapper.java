package com.bordeauxbrothers.parkshark.person.service.mappers;

import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Address;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.LicensePlate;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddMemberDTO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PersonMapper {
    PasswordEncoder passwordEncoder;

    public PersonMapper(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public Member fromAddMemberDTOtoMember(
            AddMemberDTO dto,
            Address address,
            LicensePlate licensePlate
    ) {
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

    private String hashedPassword(String password){
        return passwordEncoder.encode(password);
    }
}
