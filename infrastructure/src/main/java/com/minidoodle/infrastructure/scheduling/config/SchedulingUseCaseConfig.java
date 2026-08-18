package com.minidoodle.infrastructure.scheduling.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import tools.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.BlockSlotUseCase;
import com.minidoodle.application.scheduling.CancelMeetingUseCase;
import com.minidoodle.application.scheduling.CreateBookingUseCase;
import com.minidoodle.application.scheduling.CreateSlotUseCase;
import com.minidoodle.application.scheduling.CreateSlotsBulkUseCase;
import com.minidoodle.application.scheduling.DeleteSlotUseCase;
import com.minidoodle.application.scheduling.EventSink;
import com.minidoodle.application.scheduling.GetAvailabilityUseCase;
import com.minidoodle.application.scheduling.ListSlotsUseCase;
import com.minidoodle.application.scheduling.MeetingRepository;
import com.minidoodle.application.scheduling.OutboxEventRepository;
import com.minidoodle.application.scheduling.PublishOutboxEventUseCase;
import com.minidoodle.application.scheduling.TimeSlotRepository;
import com.minidoodle.application.scheduling.UnblockSlotUseCase;
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

    @Bean
    BlockSlotUseCase blockSlotUseCase(TimeSlotRepository timeSlotRepository) {
        return new BlockSlotUseCase(timeSlotRepository);
    }

    @Bean
    UnblockSlotUseCase unblockSlotUseCase(TimeSlotRepository timeSlotRepository) {
        return new UnblockSlotUseCase(timeSlotRepository);
    }

    @Bean
    ListSlotsUseCase listSlotsUseCase(TimeSlotRepository timeSlotRepository) {
        return new ListSlotsUseCase(timeSlotRepository);
    }

    @Bean
    GetAvailabilityUseCase getAvailabilityUseCase(TimeSlotRepository timeSlotRepository) {
        return new GetAvailabilityUseCase(timeSlotRepository);
    }

    @Bean
    CreateBookingUseCase createBookingUseCase(TimeSlotRepository timeSlotRepository,
            MeetingRepository meetingRepository, OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        return new CreateBookingUseCase(timeSlotRepository, meetingRepository, outboxEventRepository, objectMapper);
    }

    @Bean
    CancelMeetingUseCase cancelMeetingUseCase(MeetingRepository meetingRepository,
            TimeSlotRepository timeSlotRepository, OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper) {
        return new CancelMeetingUseCase(meetingRepository, timeSlotRepository, outboxEventRepository, objectMapper);
    }

    @Bean
    PublishOutboxEventUseCase publishOutboxEventUseCase(OutboxEventRepository outboxEventRepository,
            EventSink eventSink) {
        return new PublishOutboxEventUseCase(outboxEventRepository, eventSink);
    }
}
