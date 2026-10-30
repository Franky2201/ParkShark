package com.bordeauxbrothers.parkshark.person.domain;

import com.bordeauxbrothers.parkshark.person.service.PersonService;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddMemberDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/person")
public class PersonController {
    private final PersonService personService;
    PersonController(PersonService personService) {
        this.personService = personService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(@Valid @RequestBody AddMemberDTO addMemberDTO){
        personService.addMember(addMemberDTO);
    }
}
