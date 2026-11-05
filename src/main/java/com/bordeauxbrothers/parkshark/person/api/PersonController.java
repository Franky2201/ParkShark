package com.bordeauxbrothers.parkshark.person.api;

import com.bordeauxbrothers.parkshark.common.security.SecurityService;
import com.bordeauxbrothers.parkshark.person.api.dtos.AddMemberDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.GetAllMembersDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.MemberDTO;
import com.bordeauxbrothers.parkshark.person.api.dtos.MembershipDTO;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature;
import com.bordeauxbrothers.parkshark.person.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "Members",
        description = "Register and review members and their membership levels (stories ME1–ME3, ML1)."
)
@RestController
@RequestMapping(path = "/person")
public class PersonController {

    private final Logger log = LoggerFactory.getLogger(PersonController.class);
    private final PersonService personService;
    private final SecurityService securityService;

    PersonController(PersonService personService, SecurityService securityService) {
        this.personService = personService;
        this.securityService = securityService;
    }

    // ------------------------------------------------------------ ME1 + ML1

    @PostMapping(consumes = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Register as a member",
            description = """
                Stories ME1 and ML1. Registers a new member. No authentication is required.

                Requirements:
                - Name, address, email, password and licence plate are required.
                - At least one of phoneNumber or mobileNumber must be provided.
                - The email must have the form x@x.x.
                - The password must be at least 6 characters long.
                - The email must not already be in use. The InvalidMembershipException

                The membership level is optional and defaults to BRONZE.
                The server sets the registration date to today.
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Personal information, licence plate and optional membership level of the new member.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AddMemberDTO.class),
                            examples = @ExampleObject(
                                    name = "Register a member",
                                    value = """
                                        {
                                          "name": {
                                            "firstName": "Tony",
                                            "lastName": "Shark"
                                          },
                                          "address": {
                                            "street": "Bondgenotenlaan",
                                            "number": "12",
                                            "postal_code": {
                                              "zip_code": "3000",
                                              "label": "Leuven"
                                            }
                                          },
                                          "phoneNumber": "+32 16 12 34 56",
                                          "mobileNumber": "+32 470 12 34 56",
                                          "email": "tony@parkshark.be",
                                          "password": "secret123",
                                          "license_plate": {
                                            "number": "1-ABC-123",
                                            "country": "BE"
                                          },
                                          "membership": "SILVER"
                                        }
                                        """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "201",
            description = "Member registered successfully."
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request (e.g. missing name or address, invalid email, password too short, no phone number at all).",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "A member with this email address already exists.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void addMember(@Valid @RequestBody AddMemberDTO addMemberDTO) {
        log.info("Adding member with email {}", addMemberDTO.email());
        personService.addMember(addMemberDTO);
    }

    // ----------------------------------------------------------------- ME2

    @GetMapping(path = "/members", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get all members",
            description = """
                Story ME2. Returns an overview of all members, intended for managers.

                Each member only includes their ID, name, licence plate number,
                telephone number, email and registration date.
                Returns an empty list if no members exist.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Members retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = GetAllMembersDTO.class)),
                    examples = @ExampleObject(
                            value = """
                                [
                                  {
                                    "id": 7,
                                    "name": {
                                      "firstName": "Tony",
                                      "lastName": "Shark"
                                    },
                                    "licensePlateNumber": "1-ABC-123",
                                    "phoneNumber": "+32 16 12 34 56",
                                    "email": "tony@parkshark.be",
                                    "registrationDate": "2026-11-04"
                                  }
                                ]
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is missing or invalid.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The authenticated user is not allowed to see all members.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public List<GetAllMembersDTO> getAllMembers(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        log.info("Trying to get all members");
        securityService.validateAuthorization(authorization, Feature.SEE_ALL_MEMBERS);
        return personService.getAllMembers();
    }

    // ----------------------------------------------------------------- ML1

    @PutMapping(path = "/membership", consumes = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Change membership level",
            description = """
                Story ML1. Changes the membership level of the authenticated member.

                The member is identified through Basic Authentication;
                no member ID is required in the request.

                Allowed levels:
                - BRONZE: €0/month, 0% reduction, max 4 hours
                - SILVER: €10/month, 20% reduction, max 6 hours
                - GOLD: €40/month, 30% reduction, max 24 hours
                """,
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "The new membership level.",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = MembershipDTO.class),
                            examples = @ExampleObject(
                                    name = "Upgrade to Gold",
                                    value = """
                                        {
                                          "membership": "GOLD"
                                        }
                                        """
                            )
                    )
            )
    )
    @ApiResponse(
            responseCode = "200",
            description = "Membership level changed successfully."
    )
    @ApiResponse(
            responseCode = "400",
            description = "Unknown membership level.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is missing or invalid.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The authenticated user is not allowed to change a membership level.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "409",
            description = "The member already has this membership level.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public void changeMembership(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody MembershipDTO membership) {
        Person user = securityService.validateAuthorization(authorization, Feature.CHANGE_MEMBERSHIP_STATUS);
        log.info("Request to change membership of user {} to {}", user.getId(), membership.membership());
        personService.changeMembership(membership, user);
    }

    // ----------------------------------------------------------------- ME3

    @GetMapping(path = "/{id}", produces = "application/json")
    @SecurityRequirement(name = "basicAuth")
    @Operation(
            summary = "Get a member",
            description = """
                Story ME3. Returns all data of a single member, intended for managers.

                The password is never included.
                """
    )
    @ApiResponse(
            responseCode = "200",
            description = "Member retrieved successfully.",
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = MemberDTO.class),
                    examples = @ExampleObject(
                            value = """
                                {
                                  "id": 7,
                                  "name": {
                                    "firstName": "Tony",
                                    "lastName": "Shark"
                                  },
                                  "address": {
                                    "street": "Bondgenotenlaan",
                                    "number": "12",
                                    "postal_code": {
                                      "zip_code": "3000",
                                      "label": "Leuven"
                                    }
                                  },
                                  "phoneNumber": "+32 16 12 34 56",
                                  "mobileNumber": "+32 470 12 34 56",
                                  "email": "tony@parkshark.be",
                                  "license_plate": {
                                    "number": "1-ABC-123",
                                    "country": "BE"
                                  },
                                  "membership": "SILVER",
                                  "registrationDate": "2026-11-04"
                                }
                                """
                    )
            )
    )
    @ApiResponse(
            responseCode = "401",
            description = "Authentication is missing or invalid.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "403",
            description = "The authenticated user is not allowed to see members.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    @ApiResponse(
            responseCode = "404",
            description = "The member does not exist.",
            content = @Content(
                    mediaType = "application/problem+json",
                    schema = @Schema(implementation = ProblemDetail.class)
            )
    )
    public MemberDTO getMember(
            @Parameter(hidden = true)
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Parameter(description = "ID of the member.", example = "7")
            @PathVariable("id") int id) {
        log.info("Trying to get member {}", id);
        securityService.validateAuthorization(authorization, Feature.SEE_SPECIFIC_MEMBER);
        return personService.getMember(id);
    }
}