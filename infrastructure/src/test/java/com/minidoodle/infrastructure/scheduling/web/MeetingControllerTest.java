package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.minidoodle.application.scheduling.CancelMeetingUseCase;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.MeetingDetails;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeetingControllerTest {

    private final CancelMeetingUseCase cancelMeetingUseCase = mock(CancelMeetingUseCase.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MeetingController controller = new MeetingController(cancelMeetingUseCase, new MeetingWebMapper());
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void cancelMeetingReturns200WithCancelledMeeting() throws Exception {
        UUID organizerId = UUID.randomUUID();
        UUID slotId = UUID.randomUUID();
        Meeting meeting = Meeting.schedule(slotId, new MeetingDetails("Sync", "d", organizerId), List.of(), "key-1");
        meeting.cancel();
        when(cancelMeetingUseCase.execute(meeting.id(), organizerId)).thenReturn(meeting);

        mockMvc.perform(post("/meetings/{id}/cancel", meeting.id())
                        .header("X-User-Id", organizerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }
}
