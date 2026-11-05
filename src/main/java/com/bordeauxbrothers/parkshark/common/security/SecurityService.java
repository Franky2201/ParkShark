package com.bordeauxbrothers.parkshark.common.security;

import com.bordeauxbrothers.parkshark.common.security.exception.ForbiddenException;
import com.bordeauxbrothers.parkshark.common.security.exception.UnauthorizedException;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature;
import com.bordeauxbrothers.parkshark.person.repositories.PersonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SecurityService {
    private final PersonRepository personRepository;
    private final PasswordEncoder encoder;
    Logger log = LoggerFactory.getLogger(SecurityService.class);

    public SecurityService(PersonRepository personRepository, PasswordEncoder encoder) {
        this.personRepository = personRepository;
        this.encoder = encoder;
    }

    public Person validateAuthorization(String authorization, Feature feature){
        log.info("Validating authorization");
        if(authorization == null || authorization.isBlank() || !authorization.startsWith("Basic ")){
            throw new UnauthorizedException();
        }
        String strippedAuthentication = authorization.substring(6);

        String decodedCredentials;

        try {
            decodedCredentials = new String(
                    Base64.getDecoder().decode(strippedAuthentication),
                    StandardCharsets.UTF_8
            );
        } catch (IllegalArgumentException e) {
            throw new UnauthorizedException();
        }

        String[] credentials = decodedCredentials.split(":", 2);
        if (credentials.length != 2) {
            throw new UnauthorizedException();
        }
        String email = credentials[0];
        String password = credentials[1];

        log.info("Validating credentials for email {}", email);
        Person person = personRepository.findByEmail(email)
                .orElseThrow(UnauthorizedException::new);

        if (!encoder.matches(password, person.getPasswordHash())) {
            throw new UnauthorizedException();
        }

        if (!person.canHaveAccessTo(feature)) {
            throw new ForbiddenException("You don't have the rights to access this resource.");
        }

        return person;
    }
}
