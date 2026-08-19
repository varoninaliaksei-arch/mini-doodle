package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import com.minidoodle.application.scheduling.BookingResult;
import com.minidoodle.application.scheduling.CreateBookingUseCase;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.Participant;

/** Business metrics here: booking.attempts and booking.duration wrap the use case call, untouched itself. */
@RestController
class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final MeetingWebMapper mapper;

    private final Counter successCounter;
    private final Counter conflictCounter;
    private final Counter notFoundCounter;
    private final Timer bookingDuration;

    BookingController(CreateBookingUseCase createBookingUseCase, MeetingWebMapper mapper, MeterRegistry meterRegistry) {
        this.createBookingUseCase = createBookingUseCase;
        this.mapper = mapper;
        this.successCounter = Counter.builder("booking.attempts").tag("result", "success").register(meterRegistry);
        this.conflictCounter = Counter.builder("booking.attempts").tag("result", "conflict").register(meterRegistry);
        this.notFoundCounter = Counter.builder("booking.attempts").tag("result", "not_found")
                .register(meterRegistry);
        this.bookingDuration = Timer.builder("booking.duration").register(meterRegistry);
    }

    @PostMapping("/bookings")
    ResponseEntity<MeetingResponse> create(@RequestHeader("X-User-Id") UUID organizerId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateBookingRequest request) {
        MeetingDetails details = new MeetingDetails(request.title(), request.description(), organizerId);
        List<Participant> participants = request.participants().stream()
                .map(p -> new Participant(p.email(), p.displayName(), p.userId()))
                .toList();

        Timer.Sample sample = Timer.start();
        try {
            BookingResult result = createBookingUseCase.execute(request.slotId(), details, participants,
                    idempotencyKey);
            successCounter.increment();
            HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
            return ResponseEntity.status(status).body(mapper.toResponse(result.meeting()));
        } catch (SlotConflictException e) {
            conflictCounter.increment();
            throw e;
        } catch (SlotNotFoundException e) {
            notFoundCounter.increment();
            throw e;
        } finally {
            sample.stop(bookingDuration);
        }
    }
}
