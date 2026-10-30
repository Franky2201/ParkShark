package com.bordeauxbrothers.parkshark.person.service;

import com.bordeauxbrothers.parkshark.person.repository.PersonRepository;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.service.mappers.PersonMapper;
import org.springframework.stereotype.Service;

@Service
public class PersonService {
    private final PersonRepository personRepository;
    private PersonMapper personMapper;

    public PersonService(PersonRepository personRepository, PersonMapper personMapper) {
        this.personRepository = personRepository;
        this.personMapper = personMapper;
    }

    public void addMember(AddMemberDTO addMemberDTO){
        personRepository.save(personMapper.fromAddMemberDTOtoMember(addMemberDTO));
    }
}
