package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.minidoodle.application.scheduling.GetAvailabilityUseCase;
import com.minidoodle.domain.scheduling.Coverage;
import com.minidoodle.domain.scheduling.CoverageInterval;
import com.minidoodle.domain.scheduling.TimeInterval;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AvailabilityControllerTest {

    private final GetAvailabilityUseCase getAvailabilityUseCase = mock(GetAvailabilityUseCase.class);

    private MockMvc mockMvc;

    private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant END = Instant.parse("2026-01-02T00:00:00Z");

    @BeforeEach
    void setUp() {
        AvailabilityController controller = new AvailabilityController(getAvailabilityUseCase,
                new AvailabilityWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void returnsMergedCoverageIntervals() throws Exception {
        UUID ownerId = UUID.randomUUID();
        when(getAvailabilityUseCase.execute(eq(ownerId), any())).thenReturn(
                List.of(new CoverageInterval(new TimeInterval(START, END), Coverage.FREE)));

        mockMvc.perform(get("/availability")
                        .param("ownerId", ownerId.toString())
                        .param("from", START.toString())
                        .param("to", END.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].coverage").value("FREE"));

        verify(getAvailabilityUseCase).execute(eq(ownerId), any());
    }
}
