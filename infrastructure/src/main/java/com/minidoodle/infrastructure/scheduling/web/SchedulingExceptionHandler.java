package com.minidoodle.infrastructure.scheduling.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.minidoodle.application.scheduling.exception.BulkSlotLimitExceededException;
import com.minidoodle.application.scheduling.exception.MeetingNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.application.scheduling.exception.WindowTooLargeException;

/**
 * RFC 7807 mapping for the scheduling API. Each case gets its own "type"
 * URI (even the two 404s, for consistency) so a client can programmatically
 * distinguish the three different 409 causes (rule 5).
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
     * Domain guard violations (e.g. TimeSlot.book() on a non-FREE slot)
     * surface as plain IllegalStateException per the repo-wide convention —
     * mapped generically here rather than with a dedicated exception type.
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

    private static ProblemDetail problem(HttpStatus status, String typePath, String title, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setType(URI.create(typePath));
        problemDetail.setTitle(title);
        return problemDetail;
    }
}
