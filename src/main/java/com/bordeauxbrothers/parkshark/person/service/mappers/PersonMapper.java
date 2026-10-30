package com.bordeauxbrothers.parkshark.person.service.mappers;

import com.bordeauxbrothers.parkshark.person.domain.Member;
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

    public Member fromAddMemberDTOtoMember(AddMemberDTO addMemberDTO) {
        return new Member(
                addMemberDTO.getName(),
                addMemberDTO.getEmail(),
                hashedPassword(addMemberDTO.getPassword()),
                registrationDate(),
                addMemberDTO.getAddress(),
                addMemberDTO.getPhoneNumber(),
                addMemberDTO.getMobileNumber(),
                addMemberDTO.getLicensePlate(),
                addMemberDTO.getMembership()
        );
    }

    private String hashedPassword(String password){
        return passwordEncoder.encode(password);
    }
    private LocalDate registrationDate(){
        return LocalDate.now();
    }
}
