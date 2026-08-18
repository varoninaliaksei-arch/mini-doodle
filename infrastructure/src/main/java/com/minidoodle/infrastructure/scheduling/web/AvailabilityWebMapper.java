package com.minidoodle.infrastructure.scheduling.web;

import java.util.List;

import org.springframework.stereotype.Component;

import com.minidoodle.domain.scheduling.CoverageInterval;

@Component
class AvailabilityWebMapper {

    CoverageIntervalResponse toResponse(CoverageInterval interval) {
        return new CoverageIntervalResponse(interval.interval().start(), interval.interval().end(),
                interval.coverage());
    }

    List<CoverageIntervalResponse> toResponseList(List<CoverageInterval> intervals) {
        return intervals.stream().map(this::toResponse).toList();
    }
}
