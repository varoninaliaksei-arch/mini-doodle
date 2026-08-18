package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.CreateBookingUseCase;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;
import com.minidoodle.domain.scheduling.MeetingStatus;
import com.minidoodle.domain.scheduling.Participant;

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

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BookingController controller = new BookingController(createBookingUseCase, new MeetingWebMapper());
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
}
