package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minidoodle.application.scheduling.BlockSlotUseCase;
import com.minidoodle.application.scheduling.CreateSlotUseCase;
import com.minidoodle.application.scheduling.CreateSlotsBulkUseCase;
import com.minidoodle.application.scheduling.DeleteSlotUseCase;
import com.minidoodle.application.scheduling.ListSlotsUseCase;
import com.minidoodle.application.scheduling.SlotPage;
import com.minidoodle.application.scheduling.TimeSlotCursor;
import com.minidoodle.application.scheduling.UnblockSlotUseCase;
import com.minidoodle.application.scheduling.UpdateSlotUseCase;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;

@RestController
@RequestMapping("/slots")
class SlotController {

    private static final int DEFAULT_LIMIT = 50;

    private final CreateSlotUseCase createSlotUseCase;
    private final CreateSlotsBulkUseCase createSlotsBulkUseCase;
    private final UpdateSlotUseCase updateSlotUseCase;
    private final DeleteSlotUseCase deleteSlotUseCase;
    private final BlockSlotUseCase blockSlotUseCase;
    private final UnblockSlotUseCase unblockSlotUseCase;
    private final ListSlotsUseCase listSlotsUseCase;
    private final SlotWebMapper mapper;

    SlotController(CreateSlotUseCase createSlotUseCase, CreateSlotsBulkUseCase createSlotsBulkUseCase,
            UpdateSlotUseCase updateSlotUseCase, DeleteSlotUseCase deleteSlotUseCase,
            BlockSlotUseCase blockSlotUseCase, UnblockSlotUseCase unblockSlotUseCase,
            ListSlotsUseCase listSlotsUseCase, SlotWebMapper mapper) {
        this.createSlotUseCase = createSlotUseCase;
        this.createSlotsBulkUseCase = createSlotsBulkUseCase;
        this.updateSlotUseCase = updateSlotUseCase;
        this.deleteSlotUseCase = deleteSlotUseCase;
        this.blockSlotUseCase = blockSlotUseCase;
        this.unblockSlotUseCase = unblockSlotUseCase;
        this.listSlotsUseCase = listSlotsUseCase;
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

    @PostMapping("/{id}/block")
    ResponseEntity<Void> block(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID callerId,
            @RequestBody SlotVersionRequest request) {
        blockSlotUseCase.execute(id, callerId, request.version());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/unblock")
    ResponseEntity<Void> unblock(@PathVariable UUID id, @RequestHeader("X-User-Id") UUID callerId,
            @RequestBody SlotVersionRequest request) {
        unblockSlotUseCase.execute(id, callerId, request.version());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    SlotPageResponse list(@RequestParam UUID ownerId, @RequestParam Instant from, @RequestParam Instant to,
            @RequestParam(required = false) SlotStatus status,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "" + DEFAULT_LIMIT) int limit) {
        Optional<TimeSlotCursor> after = Optional.ofNullable(cursor).map(SlotCursorCodec::decode);

        SlotPage page = listSlotsUseCase.execute(ownerId, new TimeInterval(from, to), Optional.ofNullable(status),
                after, limit);

        String nextCursor = null;
        if (page.hasMore() && !page.items().isEmpty()) {
            TimeSlot last = page.items().getLast();
            nextCursor = SlotCursorCodec.encode(new TimeSlotCursor(last.interval().start(), last.id()));
        }

        return new SlotPageResponse(mapper.toResponseList(page.items()), nextCursor);
    }
}
