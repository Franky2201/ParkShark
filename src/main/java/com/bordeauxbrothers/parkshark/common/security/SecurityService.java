package com.bordeauxbrothers.parkshark.common.security;

import com.bordeauxbrothers.parkshark.common.exception.ForbiddenException;
import com.bordeauxbrothers.parkshark.common.exception.UnauthorizedException;
import com.bordeauxbrothers.parkshark.person.domain.Person;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature;
import com.bordeauxbrothers.parkshark.person.repository.PersonRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Service
public class SecurityService {
    private final PersonRepository repo;
    private final PasswordEncoder encoder;

    public SecurityService(PersonRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public void validateAuthorization(String authorization, Feature feature){
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

        Person member = repo.findByEmail(email)
                .orElseThrow(UnauthorizedException::new);

        if (!encoder.matches(password, member.getPasswordHash())) {
            throw new UnauthorizedException();
        }

        if (!member.canHaveAccessTo(feature)) {
            throw new ForbiddenException("You don't have the rights to access this resource.");
        }
    }
}
