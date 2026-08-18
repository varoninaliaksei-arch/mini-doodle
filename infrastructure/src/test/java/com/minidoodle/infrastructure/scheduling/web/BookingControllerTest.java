package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import com.minidoodle.application.scheduling.CreateBookingUseCase;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.Participant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BookingControllerTest {

    private final CreateBookingUseCase createBookingUseCase = mock(CreateBookingUseCase.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();

    private BookingController controller;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        controller = new BookingController(createBookingUseCase, new MeetingWebMapper(), meterRegistry);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createBookingReturns201WithOrganizerIdFromHeaderNotBody() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        UUID meetingId = UUID.randomUUID();
        Meeting meeting = Meeting.schedule(slotId,
                new MeetingDetails("Sync", "weekly sync", organizerId),
                List.of(new Participant("a@example.com", "Alice", null)), "key-1");
        when(createBookingUseCase.execute(eq(slotId), any(), any(), eq("key-1"))).thenReturn(meeting);

        CreateBookingRequest request = new CreateBookingRequest(slotId, "Sync", "weekly sync",
                List.of(new ParticipantRequest("a@example.com", "Alice", null)));

        mockMvc.perform(post("/bookings")
                        .header("X-User-Id", organizerId.toString())
                        .header("Idempotency-Key", "key-1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.organizerId").value(organizerId.toString()))
                .andExpect(jsonPath("$.status").value("SCHEDULED"));

        assertEquals(1.0, counter("success"));
        assertEquals(0.0, counter("conflict"));
        assertEquals(0.0, counter("not_found"));
    }

    @Test
    void idempotencyKeyIsOptional() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Meeting meeting = Meeting.schedule(slotId, new MeetingDetails("Sync", "d", organizerId), List.of(), null);
        when(createBookingUseCase.execute(eq(slotId), any(), any(), isNull())).thenReturn(meeting);

        CreateBookingRequest request = new CreateBookingRequest(slotId, "Sync", "d", List.of());

        mockMvc.perform(post("/bookings")
                        .header("X-User-Id", organizerId.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(createBookingUseCase).execute(eq(slotId), any(), any(), isNull());
    }

    @Test
    void conflictExceptionIncrementsConflictCounterAndRethrows() {
        UUID organizerId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        SlotConflictException conflict = new SlotConflictException(slotId, new RuntimeException("lost the race"));
        when(createBookingUseCase.execute(eq(slotId), any(), any(), any())).thenThrow(conflict);

        CreateBookingRequest request = new CreateBookingRequest(slotId, "Sync", "d", List.of());

        assertThrows(SlotConflictException.class,
                () -> controller.create(organizerId, "key-1", request));

        assertEquals(0.0, counter("success"));
        assertEquals(1.0, counter("conflict"));
        assertEquals(0.0, counter("not_found"));
    }

    @Test
    void notFoundExceptionIncrementsNotFoundCounterAndRethrows() {
        UUID organizerId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        when(createBookingUseCase.execute(eq(slotId), any(), any(), any()))
                .thenThrow(new SlotNotFoundException(slotId));

        CreateBookingRequest request = new CreateBookingRequest(slotId, "Sync", "d", List.of());

        assertThrows(SlotNotFoundException.class,
                () -> controller.create(organizerId, "key-1", request));

        assertEquals(0.0, counter("success"));
        assertEquals(0.0, counter("conflict"));
        assertEquals(1.0, counter("not_found"));
    }

    private double counter(String result) {
        return meterRegistry.get("booking.attempts").tag("result", result).counter().count();
    }
}
