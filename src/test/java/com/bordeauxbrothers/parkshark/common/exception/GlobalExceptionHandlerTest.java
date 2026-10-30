package com.bordeauxbrothers.parkshark.common.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bordeauxbrothers.parkshark.common.security.exception.ForbiddenException;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests {@link GlobalExceptionHandler} in isolation with Rest Assured (MockMvc module).
 * <p>No Spring context, no database, no running server, no real controllers:
 * a test-only controller throws each exception, and the handler translates it.</p>
 */
@DisplayName("GlobalExceptionHandler")
public class GlobalExceptionHandlerTest {

    private Logger handlerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(
                MockMvcBuilders.standaloneSetup(new ThrowingController())
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build());

        handlerLogger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        handlerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        // RestAssuredMockMvc configuration is static: reset it so it can't leak into other test classes
        RestAssuredMockMvc.reset();
        // reset logger
        handlerLogger.detachAppender(logAppender);
    }

    // ---------- Helper methods ----------

    /**
     * Only WARN and ERROR events: framework DEBUG noise on the same logger is ignored.
     */
    private List<ILoggingEvent> warningsAndErrors() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel().isGreaterOrEqual(Level.WARN))
                .toList();
    }

    // ---------- Our own exceptions ----------

    @Test
    @DisplayName("Requesting a resource that doesn't exist returns 404 as problem+json, with the exception message as detail")
    void givenUnknownResource_whenRequested_thenReturns404AsProblemJson() {
        given()
                .when()
                .get("/test/not-found")
                .then()
                .statusCode(404)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE)
                .body("status", equalTo(404))
                .body("detail", equalTo("Parking lot 42 not found"));
    }

    @Test
    @DisplayName("A request that conflicts with the current state (e.g. a full parking lot) returns 409 Conflict")
    void givenFullParkingLot_whenConflictIsThrown_thenReturns409() {
        given()
                .when()
                .get("/test/conflict")
                .then()
                .statusCode(409)
                .body("detail", equalTo("Parking lot is full"));
    }

    @Test
    @DisplayName("A caller acting on something they don't own returns 403 Forbidden")
    void givenNonOwner_whenForbiddenIsThrown_thenReturns403() {
        given()
                .when()
                .get("/test/forbidden")
                .then()
                .statusCode(403)
                .body("detail", equalTo("Not the owner of this allocation"));
    }

    @Test
    @DisplayName("Input that breaks a business rule returns 400 Bad Request")
    void givenInvalidBusinessInput_whenBadRequestIsThrown_thenReturns400() {
        given()
                .when()
                .get("/test/bad-request")
                .then()
                .statusCode(400)
                .body("detail", equalTo("License plate does not match"));
    }

    @Test
    @DisplayName("A ParkSharkException without its own handler falls back to 400 Bad Request")
    void givenUnmappedParkSharkException_whenThrown_thenFallsBackTo400() {
        given()
                .when()
                .get("/test/unmapped")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Nobody mapped me"));
    }

    // ---------- Unexpected errors ----------

    @Test
    @DisplayName("An unexpected exception returns 500 with a generic message and never leaks internal details")
    void givenUnexpectedException_whenThrown_thenReturns500WithoutLeakingDetails() {
        given()
                .when()
                .get("/test/boom")
                .then()
                .statusCode(500)
                .body("detail", equalTo("Something went wrong!"))
                .body(not(containsString("secret")));
    }

    @Test
    @DisplayName("An IllegalArgumentException is treated as a bug, not a client error, and returns 500")
    void givenIllegalArgumentException_whenThrown_thenReturns500() {
        given()
                .when()
                .get("/test/illegal")
                .then()
                .statusCode(500);
    }

    // ---------- Validation ----------

    @Test
    @DisplayName("A request body with an invalid field returns 400 with that field listed in the errors map")
    void givenBlankName_whenPostingMember_thenReturns400WithFieldError() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"name\": \"\"}")
                .when()
                .post("/test/members")
                .then()
                .statusCode(400)
                .body("detail", equalTo("Validation failed"))
                // only check existence: the default message depends on the machine's locale
                .body("errors.name", notNullValue());
    }

    @Test
    @DisplayName("A request body breaking a class-level rule (no mobile or telephone) returns 400 with a global error")
    void givenNoPhoneNumbers_whenPostingContact_thenReturns400WithGlobalError() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"mobile\": null, \"telephone\": null}")
                .when()
                .post("/test/contacts")
                .then()
                .statusCode(400)
                .body("errors.contactRequest", equalTo("mobile or telephone is required"));
    }

    // ---------- Spring's built-in exceptions ----------

    @Test
    @DisplayName("A request body that isn't valid JSON returns 400 Bad Request")
    void givenMalformedJson_whenPostingMember_thenReturns400() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{ this is not json")
                .when()
                .post("/test/members")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("Using an HTTP method the endpoint doesn't support returns 405 Method Not Allowed")
    void givenGetOnlyEndpoint_whenPosting_thenReturns405() {
        given()
                .when()
                .post("/test/not-found")
                .then()
                .statusCode(405);
    }

    // ---------- Logging ----------

    @Test
    @DisplayName("A client error is logged once at WARN with status, method, URI and message, without a stack trace")
    void givenUnknownResource_whenRequested_thenLogsWarnWithoutStackTrace() {
        given().when().get("/test/not-found");

        List<ILoggingEvent> events = warningsAndErrors();
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.WARN);
        assertThat(event.getFormattedMessage()).isEqualTo("404 on GET /test/not-found: Parking lot 42 not found");
        assertThat(event.getThrowableProxy()).isNull();
    }

    @Test
    @DisplayName("An unexpected error is logged once at ERROR with the full stack trace, even though the client doesn't see it")
    void givenUnexpectedException_whenThrown_thenLogsErrorWithStackTrace() {
        given().when().get("/test/boom");

        List<ILoggingEvent> events = warningsAndErrors();
        assertThat(events).hasSize(1);
        ILoggingEvent event = events.getFirst();
        assertThat(event.getLevel()).isEqualTo(Level.ERROR);
        assertThat(event.getFormattedMessage()).isEqualTo("Unexpected error on GET /test/boom");
        assertThat(event.getThrowableProxy()).isNotNull();
        assertThat(event.getThrowableProxy().getMessage()).isEqualTo("secret database details");
    }

    @Test
    @DisplayName("A validation failure is logged once at WARN, including the request path and the invalid fields")
    void givenBlankName_whenPostingMember_thenLogsWarnWithFieldErrors() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{\"name\": \"\"}")
                .when()
                .post("/test/members");

        List<ILoggingEvent> events = warningsAndErrors();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getLevel()).isEqualTo(Level.WARN);
        assertThat(events.getFirst().getFormattedMessage())
                .startsWith("Validation failed on uri=/test/members")
                .contains("name");
    }

    @Test
    @DisplayName("A built-in Spring error (malformed JSON) is also logged once at WARN via handleExceptionInternal")
    void givenMalformedJson_whenPostingMember_thenLogsWarn() {
        given()
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body("{ this is not json")
                .when()
                .post("/test/members");

        List<ILoggingEvent> events = warningsAndErrors();
        assertThat(events).hasSize(1);
        assertThat(events.getFirst().getLevel()).isEqualTo(Level.WARN);
        assertThat(events.getFirst().getFormattedMessage()).startsWith("400 on uri=/test/members");
    }

    // ---------- Test fixtures ----------

    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    @Constraint(validatedBy = AtLeastOnePhoneValidator.class)
    public @interface AtLeastOnePhone {
        String message() default "mobile or telephone is required";

        Class<?>[] groups() default {};

        Class<? extends Payload>[] payload() default {};
    }

    @RestController
    @RequestMapping("/test")
    public static class ThrowingController {

        @GetMapping("/not-found")
        public void notFound() {
            throw new ResourceNotFoundException("Parking lot 42 not found");
        }

        @GetMapping("/conflict")
        public void conflict() {
            throw new ConflictException("Parking lot is full");
        }

        @GetMapping("/forbidden")
        public void forbidden() {
            throw new ForbiddenException("Not the owner of this allocation");
        }

        @GetMapping("/bad-request")
        public void badRequest() {
            throw new BadRequestException("License plate does not match");
        }

        @GetMapping("/unmapped")
        public void unmapped() {
            throw new UnmappedException("Nobody mapped me");
        }

        @GetMapping("/boom")
        public void boom() {
            throw new RuntimeException("secret database details");
        }

        @GetMapping("/illegal")
        public void illegal() {
            throw new IllegalArgumentException("oops");
        }

        @PostMapping("/members")
        public void createMember(@Valid @RequestBody MemberRequest request) {
        }

        @PostMapping("/contacts")
        public void createContact(@Valid @RequestBody ContactRequest request) {
        }
    }

    /**
     * A ParkSharkException subclass with no dedicated handler, to test the fallback.
     */
    public static class UnmappedException extends ParkSharkException {
        public UnmappedException(String message) {
            super(message);
        }
    }

    public record MemberRequest(@NotBlank String name) {
    }

    /**
     * Mirrors story PL1: a contact person needs a mobile or a telephone number.
     */
    @AtLeastOnePhone
    public record ContactRequest(String mobile, String telephone) {
    }

    public static class AtLeastOnePhoneValidator implements ConstraintValidator<AtLeastOnePhone, ContactRequest> {
        @Override
        public boolean isValid(ContactRequest request, ConstraintValidatorContext context) {
            return request == null || request.mobile() != null || request.telephone() != null;
        }
    }
}