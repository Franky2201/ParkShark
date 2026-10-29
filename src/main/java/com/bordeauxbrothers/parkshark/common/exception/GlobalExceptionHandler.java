package com.bordeauxbrothers.parkshark.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central exception handler for the ParkShark REST API.
 * <p>Translates exceptions into RFC 9457 {@link ProblemDetail} JSON responses with a
 * meaningful HTTP status code, and logs every error:</p>
 * <ul>
 *     <li>Client errors (4xx) are logged at WARN level, without a stack trace.</li>
 *     <li>Unexpected errors (5xx) are logged at ERROR level, with the full stack trace,
 *     while the client only receives a generic message.</li>
 * </ul>
 * <p>Standard Spring MVC exceptions (malformed JSON, unsupported HTTP method, missing
 * parameters, ...) are handled by {@link ResponseEntityExceptionHandler} and logged via
 * {@link #handleExceptionInternal}.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Logs a client error at WARN level and builds the matching {@link ProblemDetail}.
     *
     * @param status the HTTP status to return
     * @param ex     the exception that was thrown
     * @param req    the current request, used for logging the method and URI
     * @return a {@code ProblemDetail} with the given status and the exception message as detail
     */
    private ProblemDetail problem(HttpStatus status, Exception ex, HttpServletRequest req) {
        log.warn("{} on {} {}: {}", status.value(), req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    }

    /**
     * Handles a requested resource that does not exist (e.g. unknown member or parking lot id).
     *
     * @param ex  the exception that was thrown
     * @param req the current request
     * @return a 404 Not Found {@code ProblemDetail}
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
        return problem(HttpStatus.NOT_FOUND, ex, req);
    }

    /**
     * Handles a request that conflicts with the current state of a resource
     * (e.g. a parking lot at full capacity, or stopping an allocation that is already stopped).
     *
     * @param ex  the exception that was thrown
     * @param req the current request
     * @return a 409 Conflict {@code ProblemDetail}
     */
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException ex, HttpServletRequest req) {
        return problem(HttpStatus.CONFLICT, ex, req);
    }

    /**
     * Handles a request the caller is not allowed to perform
     * (e.g. a member stopping a parking allocation they don't own).
     *
     * @param ex  the exception that was thrown
     * @param req the current request
     * @return a 403 Forbidden {@code ProblemDetail}
     */
    @ExceptionHandler(ForbiddenException.class)
    public ProblemDetail handleForbidden(ForbiddenException ex, HttpServletRequest req) {
        return problem(HttpStatus.FORBIDDEN, ex, req);
    }

    /**
     * Handles a request that violates a business rule
     * (e.g. a license plate that doesn't match the member's, when the member isn't Gold).
     *
     * @param ex  the exception that was thrown
     * @param req the current request
     * @return a 400 Bad Request {@code ProblemDetail}
     */
    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(BadRequestException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, ex, req);
    }

    /**
     * Fallback for any {@link ParkSharkException} subclass that has no dedicated handler.
     * <p>More specific handlers always take precedence. If this one fires, a new exception
     * type was probably added without a mapping, so consider adding a dedicated handler.</p>
     *
     * @param ex  the exception that was thrown
     * @param req the current request
     * @return a 400 Bad Request {@code ProblemDetail}
     */
    @ExceptionHandler(ParkSharkException.class)
    public ProblemDetail handleParkShark(ParkSharkException ex, HttpServletRequest req) {
        return problem(HttpStatus.BAD_REQUEST, ex, req);
    }

    /**
     * Last-resort handler for any exception not handled elsewhere. These are bugs, not client errors.
     * <p>Logs the full stack trace at ERROR level and returns a generic message,
     * so no internal details are exposed to the client.</p>
     *
     * @param ex  the exception that was thrown
     * @param req the current request
     * @return a 500 Internal Server Error {@code ProblemDetail}
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unexpected error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong!");
    }

    /**
     * Handles failed bean validation on a {@code @Valid} request body.
     * <p>Returns a 400 {@link ProblemDetail} with an {@code errors} map containing both
     * field-level errors (keyed by field name) and class-level (global) errors (keyed by object name).
     * If a field has several errors, only the first message is kept.</p>
     *
     * @param ex      the validation exception
     * @param headers the headers to be written to the response
     * @param status  the selected response status
     * @param request the current request
     * @return a 400 Bad Request response with the validation errors
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, @NonNull HttpHeaders headers, @NonNull HttpStatusCode status, @NonNull WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), String.valueOf(fe.getDefaultMessage())));
        ex.getBindingResult().getGlobalErrors()
                .forEach(ge -> errors.putIfAbsent(ge.getObjectName(), String.valueOf(ge.getDefaultMessage())));

        log.warn("Validation failed on {}: {}", request.getDescription(false), errors);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed");
        problemDetail.setProperty("errors", errors);
        return ResponseEntity.badRequest().body(problemDetail);
    }

    /**
     * Logs every standard Spring MVC exception handled by {@link ResponseEntityExceptionHandler}
     * (malformed JSON, unsupported HTTP method, missing parameters, type mismatches, ...)
     * at WARN level, then delegates to the default implementation.
     *
     * @param ex         the exception that was thrown
     * @param body       the response body, or {@code null}
     * @param headers    the headers to be written to the response
     * @param statusCode the selected response status
     * @param request    the current request
     * @return the response produced by the default implementation
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, @Nullable Object body, @NonNull HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        log.warn("{} on {}: {}", statusCode.value(), request.getDescription(false), ex.getMessage());
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }
}