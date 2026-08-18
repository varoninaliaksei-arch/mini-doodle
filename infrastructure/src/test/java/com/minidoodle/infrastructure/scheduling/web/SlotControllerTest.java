package com.minidoodle.infrastructure.scheduling.web;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.CreateSlotUseCase;
import com.minidoodle.application.scheduling.CreateSlotsBulkUseCase;
import com.minidoodle.application.scheduling.DeleteSlotUseCase;
import com.minidoodle.application.scheduling.UpdateSlotUseCase;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Standalone MockMvc: no Spring context, no DB — just the controller under test. */
class SlotControllerTest {

    private final CreateSlotUseCase createSlotUseCase = mock(CreateSlotUseCase.class);
    private final CreateSlotsBulkUseCase createSlotsBulkUseCase = mock(CreateSlotsBulkUseCase.class);
    private final UpdateSlotUseCase updateSlotUseCase = mock(UpdateSlotUseCase.class);
    private final DeleteSlotUseCase deleteSlotUseCase = mock(DeleteSlotUseCase.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;

    private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");
    private static final Instant END = Instant.parse("2026-01-01T11:00:00Z");

    @BeforeEach
    void setUp() {
        SlotController controller = new SlotController(createSlotUseCase, createSlotsBulkUseCase, updateSlotUseCase,
                deleteSlotUseCase, new SlotWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void createSlotReturns201WithOwnerIdFromHeader() throws Exception {
        UUID ownerId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        TimeSlot created = new TimeSlot(slotId, ownerId, new TimeInterval(START, END), SlotStatus.FREE, 0L);
        when(createSlotUseCase.execute(eq(ownerId), any())).thenReturn(created);

        mockMvc.perform(post("/slots")
                        .header("X-User-Id", ownerId.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new CreateSlotRequest(START, END))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(slotId.toString()))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.status").value("FREE"));

        verify(createSlotUseCase).execute(ownerId, new TimeInterval(START, END));
    }

    @Test
    void createSlotsBulkReturns201WithList() throws Exception {
        UUID ownerId = UUID.randomUUID();
        TimeSlot slot1 = new TimeSlot(UUID.randomUUID(), ownerId, new TimeInterval(START, START.plusSeconds(1800)),
                SlotStatus.FREE, 0L);
        TimeSlot slot2 = new TimeSlot(UUID.randomUUID(), ownerId,
                new TimeInterval(START.plusSeconds(1800), END), SlotStatus.FREE, 0L);
        when(createSlotsBulkUseCase.execute(eq(ownerId), any(), any())).thenReturn(List.of(slot1, slot2));

        mockMvc.perform(post("/slots/bulk")
                        .header("X-User-Id", ownerId.toString())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(
                                new CreateSlotsBulkRequest(START, END, Duration.ofMinutes(30)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void updateSlotReturns200WithUpdatedBody() throws Exception {
        UUID slotId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant newStart = START.minusSeconds(1800);
        TimeSlot updated = new TimeSlot(slotId, ownerId, new TimeInterval(newStart, END), SlotStatus.FREE, 1L);
        when(updateSlotUseCase.execute(eq(slotId), any(), any(), anyLong())).thenReturn(updated);

        mockMvc.perform(patch("/slots/{id}", slotId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new UpdateSlotRequest(newStart, null, 0L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startsAt").value(newStart.toString()))
                .andExpect(jsonPath("$.version").value(1));

        verify(updateSlotUseCase).execute(slotId, Optional.of(newStart), Optional.empty(), 0L);
    }

    @Test
    void deleteSlotReturns204() throws Exception {
        UUID slotId = UUID.randomUUID();

        mockMvc.perform(delete("/slots/{id}", slotId)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(new DeleteSlotRequest(2L))))
                .andExpect(status().isNoContent());

        verify(deleteSlotUseCase).execute(slotId, 2L);
    }
}
