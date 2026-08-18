package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;

import org.springframework.stereotype.Component;

import com.minidoodle.domain.scheduling.TimeSlot;

@Component
class SlotWebMapper {

    SlotResponse toResponse(TimeSlot slot) {
        return new SlotResponse(slot.id(), slot.calendarId().toString(), slot.interval().start(),
                slot.interval().end(), slot.status(), slot.version());
    }

    List<SlotResponse> toResponseList(List<TimeSlot> slots) {
        return slots.stream().map(this::toResponse).toList();
    }
}
