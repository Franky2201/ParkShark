package com.bordeauxbrothers.parkshark.common.security;

import com.bordeauxbrothers.parkshark.common.exception.ForbiddenException;
import com.bordeauxbrothers.parkshark.common.exception.UnauthorizedException;
import com.bordeauxbrothers.parkshark.person.domain.Manager;
import com.bordeauxbrothers.parkshark.person.domain.Member;
import com.bordeauxbrothers.parkshark.person.domain.persondetails.Feature;
import com.bordeauxbrothers.parkshark.person.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityServiceTest {
    private PersonRepository repo;
    private SecurityService securityService;
    private BCryptPasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        repo = mock(PersonRepository.class);
        encoder = new BCryptPasswordEncoder();
        securityService = new SecurityService(repo, encoder);
    }

    @Test
    void rejectsMissingAuthorizationHeader() {
        assertThrows(
                UnauthorizedException.class,
                () -> securityService.validateAuthorization(null, Feature.SEE_ALL_MEMBERS)
        );
    }

    @Test
    void rejectsMalformedBasicCredentials() {
        assertThrows(
                UnauthorizedException.class,
                () -> securityService.validateAuthorization("Basic !!!", Feature.SEE_ALL_MEMBERS)
        );
    }

    @Test
    void rejectsUnknownEmail() {
        when(repo.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThrows(
                UnauthorizedException.class,
                () -> securityService.validateAuthorization(
                        basicAuth("missing@example.com", "password"),
                        Feature.SEE_ALL_MEMBERS
                )
        );
    }

    @Test
    void rejectsWrongPassword() {
        var manager = new Manager(
                null,
                "manager@example.com",
                encoder.encode("correct-password"),
                null
        );
        when(repo.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));

        assertThrows(
                UnauthorizedException.class,
                () -> securityService.validateAuthorization(
                        basicAuth("manager@example.com", "wrong-password"),
                        Feature.SEE_ALL_MEMBERS
                )
        );
    }

    @Test
    void rejectsUserWithoutRequiredFeature() {
        var member = new Member(
                null,
                "member@example.com",
                encoder.encode("password"),
                null,
                null, null, null, null, null
        );
        when(repo.findByEmail("member@example.com")).thenReturn(Optional.of(member));

        assertThrows(
                ForbiddenException.class,
                () -> securityService.validateAuthorization(
                        basicAuth("member@example.com", "password"),
                        Feature.SEE_ALL_MEMBERS
                )
        );
    }

    @Test
    void acceptsValidCredentialsAndFeature() {
        var manager = new Manager(
                null,
                "manager@example.com",
                encoder.encode("password"),
                null
        );
        when(repo.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));

        assertDoesNotThrow(() ->
                securityService.validateAuthorization(
                        basicAuth("manager@example.com", "password"),
                        Feature.SEE_ALL_MEMBERS
                )
        );
    }

    private String basicAuth(String email, String password) {
        String credentials = email + ":" + password;
        String encoded = Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Basic " + encoded;
    }
}