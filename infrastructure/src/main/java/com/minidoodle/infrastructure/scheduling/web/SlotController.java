package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.minidoodle.application.scheduling.CreateSlotUseCase;
import com.minidoodle.application.scheduling.CreateSlotsBulkUseCase;
import com.minidoodle.application.scheduling.DeleteSlotUseCase;
import com.minidoodle.application.scheduling.UpdateSlotUseCase;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

@RestController
@RequestMapping("/slots")
class SlotController {

    private final CreateSlotUseCase createSlotUseCase;
    private final CreateSlotsBulkUseCase createSlotsBulkUseCase;
    private final UpdateSlotUseCase updateSlotUseCase;
    private final DeleteSlotUseCase deleteSlotUseCase;
    private final SlotWebMapper mapper;

    SlotController(CreateSlotUseCase createSlotUseCase, CreateSlotsBulkUseCase createSlotsBulkUseCase,
            UpdateSlotUseCase updateSlotUseCase, DeleteSlotUseCase deleteSlotUseCase, SlotWebMapper mapper) {
        this.createSlotUseCase = createSlotUseCase;
        this.createSlotsBulkUseCase = createSlotsBulkUseCase;
        this.updateSlotUseCase = updateSlotUseCase;
        this.deleteSlotUseCase = deleteSlotUseCase;
        this.mapper = mapper;
    }

    @PostMapping
    ResponseEntity<SlotResponse> create(@RequestHeader("X-User-Id") UUID ownerId,
            @RequestBody CreateSlotRequest request) {
        TimeSlot slot = createSlotUseCase.execute(ownerId, new TimeInterval(request.startsAt(), request.endsAt()));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(slot));
    }

    @PostMapping("/bulk")
    ResponseEntity<List<SlotResponse>> createBulk(@RequestHeader("X-User-Id") UUID ownerId,
            @RequestBody CreateSlotsBulkRequest request) {
        List<TimeSlot> slots = createSlotsBulkUseCase.execute(ownerId,
                new TimeInterval(request.startsAt(), request.endsAt()), request.slotDuration());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponseList(slots));
    }

    @PatchMapping("/{id}")
    SlotResponse update(@PathVariable UUID id, @RequestBody UpdateSlotRequest request) {
        TimeSlot updated = updateSlotUseCase.execute(id, Optional.ofNullable(request.startsAt()),
                Optional.ofNullable(request.endsAt()), request.version());
        return mapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    ResponseEntity<Void> delete(@PathVariable UUID id, @RequestBody DeleteSlotRequest request) {
        deleteSlotUseCase.execute(id, request.version());
        return ResponseEntity.noContent().build();
    }
}
