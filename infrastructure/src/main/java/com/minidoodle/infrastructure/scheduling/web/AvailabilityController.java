package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.minidoodle.application.scheduling.GetAvailabilityUseCase;
import com.minidoodle.domain.scheduling.CoverageInterval;
import com.minidoodle.domain.scheduling.TimeInterval;

@RestController
class AvailabilityController {

    private final GetAvailabilityUseCase getAvailabilityUseCase;
    private final AvailabilityWebMapper mapper;

    AvailabilityController(GetAvailabilityUseCase getAvailabilityUseCase, AvailabilityWebMapper mapper) {
        this.getAvailabilityUseCase = getAvailabilityUseCase;
        this.mapper = mapper;
    }

    @GetMapping("/availability")
    List<CoverageIntervalResponse> get(@RequestParam UUID ownerId, @RequestParam Instant from,
            @RequestParam Instant to) {
        List<CoverageInterval> coverage = getAvailabilityUseCase.execute(ownerId, new TimeInterval(from, to));
        return mapper.toResponseList(coverage);
    }
}
