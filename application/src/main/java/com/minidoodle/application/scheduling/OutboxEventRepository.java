package com.minidoodle.application.scheduling;

public interface OutboxEventRepository {

    void save(OutboxEvent event);
}
