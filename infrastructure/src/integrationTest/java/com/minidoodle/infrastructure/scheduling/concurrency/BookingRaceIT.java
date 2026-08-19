package com.minidoodle.infrastructure.scheduling.concurrency;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.minidoodle.application.scheduling.MeetingRepository;
import com.minidoodle.application.scheduling.TimeSlotRepository;
import com.minidoodle.domain.scheduling.Meeting;
import com.minidoodle.domain.scheduling.SlotStatus;
import com.minidoodle.domain.scheduling.TimeInterval;
import com.minidoodle.domain.scheduling.TimeSlot;
import com.minidoodle.infrastructure.scheduling.web.CreateBookingRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Proves double-booking is actually prevented under real concurrency, not
 * just documented intent.
 *
 * <p>This is a classic write-skew scenario — an invariant ("a slot has at
 * most one active meeting") spanning two rows with no natural row owner —
 * which READ_COMMITTED does not catch on its own. Two independent
 * transactions can each read the slot as FREE before either commits; only
 * a real serialization point at the database decides the winner. Real
 * concurrent HTTP against a real Postgres instance is the only way to
 * prove that: a MockMvc test runs the whole stack synchronously on one
 * thread, and an in-memory fake repository has no serialization behavior
 * to get wrong in the first place.
 *
 * <p>A failing assertion here is a real correctness bug in the
 * concurrency control or its exception mapping, not a timing flake — no
 * retry/backoff logic exists to coax a passing result.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BookingRaceIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private static final int CONCURRENCY = 50;

    @LocalServerPort
    private int port;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private UUID ownerId;
    private UUID slotId;

    @BeforeEach
    void seedFreeSlot() {
        ownerId = UUID.randomUUID();
        Instant start = Instant.now().plus(Duration.ofDays(1)).truncatedTo(ChronoUnit.SECONDS);
        TimeSlot slot = TimeSlot.free(UUID.randomUUID(), ownerId,
                new TimeInterval(start, start.plus(Duration.ofHours(1))));
        timeSlotRepository.save(slot);
        slotId = slot.id();
    }

    @Test
    void exactlyOneOfManyConcurrentBookingsOnTheSameSlotSucceeds() throws InterruptedException {
        List<HttpResponse<String>> responses = fireConcurrentBookings(idx -> "distinct-" + UUID.randomUUID());

        List<HttpResponse<String>> winners = responses.stream().filter(r -> r.statusCode() == 201).toList();
        List<HttpResponse<String>> losers = responses.stream().filter(r -> r.statusCode() != 201).toList();

        assertEquals(1, winners.size(), () -> "expected exactly one 201; got:\n" + summarize(responses));
        assertEquals(CONCURRENCY - 1, losers.size(), () -> "expected exactly " + (CONCURRENCY - 1)
                + " non-201 responses; got:\n" + summarize(responses));

        for (HttpResponse<String> loser : losers) {
            assertEquals(409, loser.statusCode(),
                    () -> "expected every loser to fail with 409, got " + loser.statusCode() + ": " + loser.body());
        }

        TimeSlot finalSlot = timeSlotRepository.findById(slotId).orElseThrow();
        assertEquals(SlotStatus.BOOKED, finalSlot.status(), "slot must end up BOOKED");

        List<Meeting> meetingsForSlot = meetingRepository.findBySlotId(slotId);
        assertEquals(1, meetingsForSlot.size(),
                () -> "expected exactly one Meeting row for the slot, found: " + meetingsForSlot);
    }

    /**
     * The Idempotency-Key header protects retry/double-click of one
     * caller — it is not a distributed lock across independent concurrent
     * actors. When two transactions race for the same key, JPA leaves no
     * way to read the winner's row from inside the loser's now-rollback-only
     * transaction (see CreateBookingUseCase), so a loser that reaches the
     * write gets a clean 409, exactly as it would for any other lost
     * slot-booking race.
     *
     * <p>Note what this test does <em>not</em> assume: that exactly one of
     * the 50 requests reaches the write path at all. The CountDownLatch
     * only synchronizes when each thread *starts*; a thread starting a
     * few microseconds later can hit the idempotency pre-check *after* an
     * earlier thread has already committed, and correctly short-circuits
     * to 201 with the existing meeting instead of racing into conflict.
     * Both outcomes are correct — this test actually proves that every
     * 201 carries the same meeting id, and that every 409 resolves to
     * that same meeting when retried (the retry loop runs unconditionally
     * after the burst settles, not to coax a pass).
     */
    @Test
    void concurrentIdenticalIdempotentBookingsNeverProduceMoreThanOneMeeting() throws Exception {
        String idempotencyKey = "race-idem-" + UUID.randomUUID();

        List<HttpResponse<String>> responses = fireConcurrentBookings(idx -> idempotencyKey);

        for (HttpResponse<String> response : responses) {
            int status = response.statusCode();
            if (status != 201 && status != 409) {
                fail("expected every response to be 201 or 409, got " + status + ": " + response.body());
            }
        }

        List<String> distinctWinningMeetingIds = responses.stream()
                .filter(r -> r.statusCode() == 201)
                .map(r -> readMeetingId(r.body()))
                .distinct()
                .toList();
        assertEquals(1, distinctWinningMeetingIds.size(),
                () -> "expected every 201 response to carry the same meeting id; got: " + distinctWinningMeetingIds
                        + "\nfull responses:\n" + summarize(responses));
        String winningMeetingId = distinctWinningMeetingIds.get(0);

        List<HttpResponse<String>> initialLosers = responses.stream().filter(r -> r.statusCode() == 409).toList();
        for (int i = 0; i < initialLosers.size(); i++) {
            HttpResponse<String> retry = sendBookingRequest(idempotencyKey, "Retry-" + i);
            assertEquals(201, retry.statusCode(), () -> "expected retry to succeed: " + retry.body());
            assertEquals(winningMeetingId, readMeetingId(retry.body()),
                    "retry must return the same meeting the original winner got");
        }

        TimeSlot finalSlot = timeSlotRepository.findById(slotId).orElseThrow();
        assertEquals(SlotStatus.BOOKED, finalSlot.status(), "slot must end up BOOKED");

        List<Meeting> meetingsForSlot = meetingRepository.findBySlotId(slotId);
        assertEquals(1, meetingsForSlot.size(),
                () -> "expected exactly one Meeting row despite the race plus " + initialLosers.size()
                        + " retries, found: " + meetingsForSlot);
    }

    /**
     * Starts {@value #CONCURRENCY} threads, holds them at a latch until
     * every thread has reached the starting line, then releases them all
     * at once — a fixed thread pool + CountDownLatch, not a sequential
     * loop, so requests actually overlap on the database.
     */
    private List<HttpResponse<String>> fireConcurrentBookings(IntFunction<String> idempotencyKeyFor)
            throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        CountDownLatch ready = new CountDownLatch(CONCURRENCY);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<HttpResponse<String>>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < CONCURRENCY; i++) {
                int idx = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return sendBookingRequest(idempotencyKeyFor.apply(idx), "Racer-" + idx);
                }));
            }

            if (!ready.await(10, TimeUnit.SECONDS)) {
                fail("racer threads did not all reach the starting line in time");
            }
            start.countDown();

            List<HttpResponse<String>> responses = new ArrayList<>();
            for (Future<HttpResponse<String>> future : futures) {
                try {
                    responses.add(future.get(30, TimeUnit.SECONDS));
                } catch (Exception e) {
                    throw new AssertionError("booking request failed", e);
                }
            }
            return responses;
        } finally {
            pool.shutdown();
        }
    }

    private HttpResponse<String> sendBookingRequest(String idempotencyKey, String title) throws Exception {
        CreateBookingRequest body = new CreateBookingRequest(slotId, title, "race test", List.of());
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/bookings"))
                .header("X-User-Id", ownerId.toString())
                .header("Idempotency-Key", idempotencyKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String readMeetingId(String responseBody) {
        try {
            JsonNode node = objectMapper.readTree(responseBody);
            return node.get("id").asText();
        } catch (Exception e) {
            throw new AssertionError("Failed to parse response body: " + responseBody, e);
        }
    }

    private static String summarize(List<HttpResponse<String>> responses) {
        return responses.stream()
                .map(r -> r.statusCode() + ": " + r.body())
                .collect(Collectors.joining("\n"));
    }
}
