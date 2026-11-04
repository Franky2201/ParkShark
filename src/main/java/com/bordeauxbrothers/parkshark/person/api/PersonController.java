package com.bordeauxbrothers.parkshark.person.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Membership;
import com.bordeauxbrothers.parkshark.person.service.PersonService;
import com.bordeauxbrothers.parkshark.person.service.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.service.dtos.GetAllMembersDTO;
import com.bordeauxbrothers.parkshark.person.service.dtos.MembershipDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/person")
public class PersonController {
    private final PersonService personService;
    private final SecurityService securityService;
    PersonController(PersonService personService, SecurityService securityService) {
        this.personService = personService;
        this.securityService = securityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(@Valid @RequestBody AddMemberDTO addMemberDTO){
        personService.addMember(addMemberDTO);
    }

    @GetMapping(path = "/members", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public List<GetAllMembersDTO>getAllMembers(@RequestHeader(value = "Authorization", required = false) String authorization){
        securityService.validateAuthorization(authorization, Feature.SEE_ALL_MEMBERS);
        return personService.getAllMembers();
    }

    @PostMapping(path = "/membership")
    @ResponseStatus(HttpStatus.OK)
    public void changeMembership(@RequestBody MembershipDTO membership, @RequestHeader(value = "Authorization", required = false) String authorization){
        Person user = securityService.validateAuthorization(authorization, Feature.CHANGE_MEMBERSHIP_STATUS);
        personService.changeMembership(membership, user);
    }
}
