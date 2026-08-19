package com.minidoodle.infrastructure.scheduling.web;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;

import com.minidoodle.application.scheduling.GetAvailabilityUseCase;
import com.minidoodle.domain.scheduling.CoverageInterval;
import com.minidoodle.domain.scheduling.TimeInterval;

@RestController
class AvailabilityController {

    private final GetAvailabilityUseCase getAvailabilityUseCase;
    private final AvailabilityWebMapper mapper;
    private final DistributionSummary windowDaysSummary;

    AvailabilityController(GetAvailabilityUseCase getAvailabilityUseCase, AvailabilityWebMapper mapper,
            MeterRegistry meterRegistry) {
        this.getAvailabilityUseCase = getAvailabilityUseCase;
        this.mapper = mapper;
        // Bounds match TECH-7's 90-day cap. Without them, Micrometer's default
        // percentile-histogram bucket generator has no domain to size itself
        // to and produces ~280 exponentially-growing buckets up to ~4.2e18 -
        // technically correct (the recorded value itself is fine, always a
        // whole day count) but useless as a heatmap Y-axis.
        this.windowDaysSummary = DistributionSummary.builder("freebusy.query.window.days")
                .minimumExpectedValue(1.0)
                .maximumExpectedValue(90.0)
                .register(meterRegistry);
    }

    @GetMapping("/availability")
    List<CoverageIntervalResponse> get(@RequestParam UUID ownerId, @RequestParam Instant from,
            @RequestParam Instant to) {
        // Recorded before WindowPolicy is enforced inside the use case, so rejected
        // oversized windows still show up in the metric's distribution.
        windowDaysSummary.record(ChronoUnit.DAYS.between(from, to));
        List<CoverageInterval> coverage = getAvailabilityUseCase.execute(ownerId, new TimeInterval(from, to));
        return mapper.toResponseList(coverage);
    }
}
