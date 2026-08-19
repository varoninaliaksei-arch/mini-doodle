package com.minidoodle.infrastructure.scheduling.web;

import java.net.URI;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.minidoodle.application.scheduling.exception.BulkSlotLimitExceededException;
import com.minidoodle.application.scheduling.exception.MeetingNotFoundException;
import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotBlockedException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.application.scheduling.exception.WindowTooLargeException;

/**
 * RFC 7807 mapping for the scheduling API. Each case gets its own "type"
 * URI (even the two 404s, for consistency) so a client can programmatically
 * distinguish the three different 409 causes.
 */
@RestControllerAdvice
class SchedulingExceptionHandler {

    @ExceptionHandler(SlotConflictException.class)
    ProblemDetail handleSlotConflict(SlotConflictException e) {
        return problem(HttpStatus.CONFLICT, "/problems/slot-conflict", "Slot conflict", e.getMessage());
    }

    @ExceptionHandler(SlotBookedException.class)
    ProblemDetail handleSlotBooked(SlotBookedException e) {
        return problem(HttpStatus.CONFLICT, "/problems/slot-booked", "Slot is booked", e.getMessage());
    }

    @ExceptionHandler(SlotBlockedException.class)
    ProblemDetail handleSlotBlocked(SlotBlockedException e) {
        return problem(HttpStatus.CONFLICT, "/problems/slot-blocked", "Slot blocked", e.getMessage());
    }

    @ExceptionHandler(SlotVersionConflictException.class)
    ProblemDetail handleSlotVersionConflict(SlotVersionConflictException e) {
        return problem(HttpStatus.CONFLICT, "/problems/slot-version-conflict", "Slot version conflict",
                e.getMessage());
    }

    @ExceptionHandler(SlotNotFoundException.class)
    ProblemDetail handleSlotNotFound(SlotNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "/problems/slot-not-found", "Slot not found", e.getMessage());
    }

    @ExceptionHandler(MeetingNotFoundException.class)
    ProblemDetail handleMeetingNotFound(MeetingNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "/problems/meeting-not-found", "Meeting not found", e.getMessage());
    }

    @ExceptionHandler(NotOwnerException.class)
    ProblemDetail handleNotOwner(NotOwnerException e) {
        return problem(HttpStatus.FORBIDDEN, "/problems/not-owner", "Not owner", e.getMessage());
    }

    @ExceptionHandler(BulkSlotLimitExceededException.class)
    ProblemDetail handleBulkSlotLimitExceeded(BulkSlotLimitExceededException e) {
        return problem(HttpStatus.UNPROCESSABLE_CONTENT, "/problems/bulk-slot-limit-exceeded",
                "Bulk slot limit exceeded", e.getMessage());
    }

    @ExceptionHandler(WindowTooLargeException.class)
    ProblemDetail handleWindowTooLarge(WindowTooLargeException e) {
        return problem(HttpStatus.BAD_REQUEST, "/problems/window-too-large", "Window too large", e.getMessage());
    }

    /**
     * Fallback for domain guard violations that a use case didn't
     * pre-empt with a dedicated exception type (e.g. an already-BLOCKED
     * slot rejecting a second block() call). Conditions with product-level
     * meaning are translated to a specific exception by their use case
     * before reaching here — see SlotBookedException, SlotConflictException.
     */
    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail handleIllegalState(IllegalStateException e) {
        return problem(HttpStatus.CONFLICT, "/problems/invalid-state-transition", "Invalid state transition",
                e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        return problem(HttpStatus.BAD_REQUEST, "/problems/invalid-request", "Invalid request", e.getMessage());
    }

    /**
     * Request body missing entirely or unparsable as JSON — Spring MVC
     * rejects this during binding, before the request ever reaches a use
     * case. Without this handler it falls through to Spring Boot's default
     * Whitelabel error shape instead of {@link ProblemDetail} like every
     * other error response in this API.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail handleMessageNotReadable(HttpMessageNotReadableException e) {
        return problem(HttpStatus.BAD_REQUEST, "/problems/validation-error", "Validation error",
                "Request body is missing or malformed: " + e.getMostSpecificCause().getMessage());
    }

    /** {@code @Valid}-annotated request body failed bean validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "/problems/validation-error", "Validation error", detail);
    }

    /** A required {@code @RequestParam} (e.g. {@code ownerId}, {@code from}) is missing. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail handleMissingRequestParameter(MissingServletRequestParameterException e) {
        return problem(HttpStatus.BAD_REQUEST, "/problems/validation-error", "Validation error", e.getMessage());
    }

    /** A path/query parameter couldn't be converted to its declared type (e.g. a malformed UUID or Instant). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return problem(HttpStatus.BAD_REQUEST, "/problems/validation-error", "Validation error", e.getMessage());
    }

    private static ProblemDetail problem(HttpStatus status, String typePath, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(typePath));
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
