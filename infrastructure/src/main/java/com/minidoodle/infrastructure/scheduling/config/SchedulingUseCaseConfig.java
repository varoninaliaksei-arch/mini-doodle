package com.minidoodle.infrastructure.scheduling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.minidoodle.application.scheduling.CreateSlotUseCase;
import com.minidoodle.application.scheduling.CreateSlotsBulkUseCase;
import com.minidoodle.application.scheduling.DeleteSlotUseCase;
import com.minidoodle.application.scheduling.TimeSlotRepository;
import com.minidoodle.application.scheduling.UpdateSlotUseCase;

/**
 * Application-layer use cases stay plain (unannotated) classes per ARCH-3 —
 * this is the one place infrastructure wires them as beans.
 */
@Configuration
class SchedulingUseCaseConfig {

    @Bean
    CreateSlotUseCase createSlotUseCase(TimeSlotRepository timeSlotRepository) {
        return new CreateSlotUseCase(timeSlotRepository);
    }

    @Bean
    CreateSlotsBulkUseCase createSlotsBulkUseCase(TimeSlotRepository timeSlotRepository) {
        return new CreateSlotsBulkUseCase(timeSlotRepository);
    }

    @Bean
    UpdateSlotUseCase updateSlotUseCase(TimeSlotRepository timeSlotRepository) {
        return new UpdateSlotUseCase(timeSlotRepository);
    }

    @Bean
    DeleteSlotUseCase deleteSlotUseCase(TimeSlotRepository timeSlotRepository) {
        return new DeleteSlotUseCase(timeSlotRepository);
    }
}
