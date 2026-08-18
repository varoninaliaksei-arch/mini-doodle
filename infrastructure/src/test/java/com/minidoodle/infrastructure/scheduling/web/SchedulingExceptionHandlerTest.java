package com.minidoodle.infrastructure.scheduling.web;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.BlockSlotUseCase;
import com.minidoodle.application.scheduling.CancelMeetingUseCase;
import com.minidoodle.application.scheduling.CreateSlotUseCase;
import com.minidoodle.application.scheduling.CreateSlotsBulkUseCase;
import com.minidoodle.application.scheduling.DeleteSlotUseCase;
import com.minidoodle.application.scheduling.ListSlotsUseCase;
import com.minidoodle.application.scheduling.UnblockSlotUseCase;
import com.minidoodle.application.scheduling.UpdateSlotUseCase;
import com.minidoodle.application.scheduling.exception.BulkSlotLimitExceededException;
import com.minidoodle.application.scheduling.exception.MeetingNotFoundException;
import com.minidoodle.application.scheduling.exception.NotOwnerException;
import com.minidoodle.application.scheduling.exception.SlotBookedException;
import com.minidoodle.application.scheduling.exception.SlotConflictException;
import com.minidoodle.application.scheduling.exception.SlotNotFoundException;
import com.minidoodle.application.scheduling.exception.SlotVersionConflictException;
import com.minidoodle.application.scheduling.exception.WindowTooLargeException;
import com.minidoodle.domain.scheduling.TimeInterval;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Covers every exception -> ProblemDetail mapping in SchedulingExceptionHandler. */
class SchedulingExceptionHandlerTest {

    private final CreateSlotUseCase createSlotUseCase = mock(CreateSlotUseCase.class);
    private final CreateSlotsBulkUseCase createSlotsBulkUseCase = mock(CreateSlotsBulkUseCase.class);
    private final UpdateSlotUseCase updateSlotUseCase = mock(UpdateSlotUseCase.class);
    private final DeleteSlotUseCase deleteSlotUseCase = mock(DeleteSlotUseCase.class);
    private final BlockSlotUseCase blockSlotUseCase = mock(BlockSlotUseCase.class);
    private final UnblockSlotUseCase unblockSlotUseCase = mock(UnblockSlotUseCase.class);
    private final ListSlotsUseCase listSlotsUseCase = mock(ListSlotsUseCase.class);
    private final CancelMeetingUseCase cancelMeetingUseCase = mock(CancelMeetingUseCase.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private static final UUID SLOT_ID = UUID.randomUUID();
    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant END = Instant.parse("2026-01-01T11:00:00Z");

    @BeforeEach
    void setUp() {
        SlotController slotController = new SlotController(createSlotUseCase, createSlotsBulkUseCase,
                updateSlotUseCase, deleteSlotUseCase, blockSlotUseCase, unblockSlotUseCase, listSlotsUseCase,
                new SlotWebMapper());
        MeetingController meetingController = new MeetingController(cancelMeetingUseCase, new MeetingWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(slotController, meetingController)
                .setControllerAdvice(new SchedulingExceptionHandler())
                .build();
    }

    @Test
    void slotConflictMapsTo409WithDistinctType() throws Exception {
        when(createSlotUseCase.execute(any(), any()))
                .thenThrow(new SlotConflictException(SLOT_ID, new TimeInterval(START, END), new RuntimeException()));

        mockMvc.perform(post("/slots")
                        .header("X-User-Id", SLOT_ID.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateSlotRequest(START, END))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/slot-conflict"));
    }

    @Test
    void slotBookedMapsTo409WithDistinctType() throws Exception {
        when(updateSlotUseCase.execute(any(), any(), any(), any(), anyLong()))
                .thenThrow(new SlotBookedException(SLOT_ID));

        mockMvc.perform(patch("/slots/{id}", SLOT_ID)
                        .header("X-User-Id", SLOT_ID.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateSlotRequest(null, null, 0L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/slot-booked"));
    }

    @Test
    void slotVersionConflictMapsTo409WithDistinctType() throws Exception {
        doThrow(new SlotVersionConflictException(SLOT_ID, 1L, 2L))
                .when(deleteSlotUseCase).execute(any(), any(), anyLong());

        mockMvc.perform(delete("/slots/{id}", SLOT_ID)
                        .header("X-User-Id", SLOT_ID.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new DeleteSlotRequest(1L))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/slot-version-conflict"));
    }

    @Test
    void slotNotFoundMapsTo404() throws Exception {
        when(updateSlotUseCase.execute(any(), any(), any(), any(), anyLong()))
                .thenThrow(new SlotNotFoundException(SLOT_ID));

        mockMvc.perform(patch("/slots/{id}", SLOT_ID)
                        .header("X-User-Id", SLOT_ID.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateSlotRequest(null, null, 0L))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/slot-not-found"));
    }

    @Test
    void notOwnerMapsTo403() throws Exception {
        UUID callerId = UUID.randomUUID();
        when(blockSlotUseCase.execute(any(), any(), anyLong())).thenThrow(new NotOwnerException("slot", SLOT_ID));

        mockMvc.perform(post("/slots/{id}/block", SLOT_ID)
                        .header("X-User-Id", callerId.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new SlotVersionRequest(0L))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("/problems/not-owner"));
    }

    @Test
    void meetingNotFoundMapsTo404() throws Exception {
        UUID meetingId = UUID.randomUUID();
        UUID callerId = UUID.randomUUID();
        when(cancelMeetingUseCase.execute(meetingId, callerId)).thenThrow(new MeetingNotFoundException(meetingId));

        mockMvc.perform(post("/meetings/{id}/cancel", meetingId)
                        .header("X-User-Id", callerId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("/problems/meeting-not-found"));
    }

    @Test
    void bulkSlotLimitExceededMapsTo422() throws Exception {
        when(createSlotsBulkUseCase.execute(any(), any(), any()))
                .thenThrow(new BulkSlotLimitExceededException(600, 500));

        mockMvc.perform(post("/slots/bulk")
                        .header("X-User-Id", SLOT_ID.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateSlotsBulkRequest(START, END, Duration.ofMinutes(1)))))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.type").value("/problems/bulk-slot-limit-exceeded"));
    }

    @Test
    void windowTooLargeMapsTo400() throws Exception {
        when(listSlotsUseCase.execute(any(), any(), any(), any(), anyInt()))
                .thenThrow(new WindowTooLargeException(Duration.ofDays(91), Duration.ofDays(90)));

        mockMvc.perform(get("/slots")
                        .param("ownerId", SLOT_ID.toString())
                        .param("from", START.toString())
                        .param("to", START.plusSeconds(91L * 24 * 3600).toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/window-too-large"));
    }

    @Test
    void illegalStateFromDomainGuardMapsTo409() throws Exception {
        when(createSlotUseCase.execute(any(), any())).thenThrow(new IllegalStateException("already booked"));

        mockMvc.perform(post("/slots")
                        .header("X-User-Id", SLOT_ID.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateSlotRequest(START, END))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("/problems/invalid-state-transition"));
    }

    @Test
    void illegalArgumentMapsTo400() throws Exception {
        when(createSlotUseCase.execute(any(), any())).thenThrow(new IllegalArgumentException("bad input"));

        mockMvc.perform(post("/slots")
                        .header("X-User-Id", SLOT_ID.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateSlotRequest(START, END))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("/problems/invalid-request"));
    }
}
