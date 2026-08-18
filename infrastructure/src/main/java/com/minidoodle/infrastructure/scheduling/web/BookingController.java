package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.minidoodle.application.scheduling.CreateBookingUseCase;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.Participant;

@RestController
class BookingController {

    private final CreateBookingUseCase createBookingUseCase;
    private final MeetingWebMapper mapper;

    BookingController(CreateBookingUseCase createBookingUseCase, MeetingWebMapper mapper) {
        this.createBookingUseCase = createBookingUseCase;
        this.mapper = mapper;
    }

    @PostMapping("/bookings")
    ResponseEntity<MeetingResponse> create(@RequestHeader("X-User-Id") UUID organizerId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody CreateBookingRequest request) {
        MeetingDetails details = new MeetingDetails(request.title(), request.description(), organizerId);
        List<Participant> participants = request.participants().stream()
                .map(p -> new Participant(p.email(), p.displayName(), p.userId()))
                .toList();

        Meeting meeting = createBookingUseCase.execute(request.slotId(), details, participants, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(meeting));
    }
}
