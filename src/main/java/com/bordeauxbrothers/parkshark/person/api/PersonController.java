package com.bordeauxbrothers.parkshark.person.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature;
import com.bordeauxbrothers.parkshark.person.service.PersonService;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.GetAllMembersDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.MembershipDTO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/person")
public class PersonController {
    private Logger log = LoggerFactory.getLogger(PersonController.class);
    private final PersonService personService;
    private final SecurityService securityService;

    PersonController(PersonService personService, SecurityService securityService) {
        this.personService = personService;
        this.securityService = securityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addMember(@Valid @RequestBody AddMemberDTO addMemberDTO){
        log.info("Adding member {}", addMemberDTO);
        personService.addMember(addMemberDTO);
    }

    @GetMapping(path = "/members", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public List<GetAllMembersDTO>getAllMembers(@RequestHeader(value = "Authorization", required = false) String authorization){
        log.info("Trying to get all members");
        securityService.validateAuthorization(authorization, Feature.SEE_ALL_MEMBERS);
        return personService.getAllMembers();
    }

    @PutMapping(path = "/membership")
    @ResponseStatus(HttpStatus.OK)
    public void changeMembership(@RequestBody MembershipDTO membership, @RequestHeader(value = "Authorization", required = false) String authorization){
        Person user = securityService.validateAuthorization(authorization, Feature.CHANGE_MEMBERSHIP_STATUS);
        log.info("request to change membership of user {} to {}", user.getId(), membership.getMembership());
        personService.changeMembership(membership, user);
    }

    @GetMapping(path = "/{id}", produces = "application/json")
    @ResponseStatus(HttpStatus.OK)
    public Member getMember(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable("id") int id){
        log.info("Trying to get member {}", id);
        securityService.validateAuthorization(authorization, Feature.SEE_SPECIFIC_MEMBER);
        return personService.getMember(id);
    }
}
