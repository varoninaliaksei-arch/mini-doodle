package com.minidoodle.infrastructure.scheduling.web;

/** DELETE /slots/{id}: version travels in the body, not an ETag/If-Match header. */
public record DeleteSlotRequest(long version) {
}
