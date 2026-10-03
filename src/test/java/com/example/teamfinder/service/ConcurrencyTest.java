package com.example.teamfinder.service;


import com.example.teamfinder.dto.event.CreateEventRequest;
import com.example.teamfinder.dto.event.EventResponse;
import com.example.teamfinder.dto.user.ParticipantResponse;
import com.example.teamfinder.model.Event;
import com.example.teamfinder.model.Role;
import com.example.teamfinder.model.SportType;
import com.example.teamfinder.model.User;
import com.example.teamfinder.repository.EventRepository;
import com.example.teamfinder.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.checkerframework.checker.units.qual.A;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ConcurrencyTest {

    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @BeforeAll
    static void beforeAll() {
        postgres.start();
    }

    @AfterAll
    static void afterAll() {
        postgres.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private EventService eventService;
    private User organizer;
    private List<User> users;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        organizer = userRepository.save(buildUser("organizer"));
        users = IntStream.range(0, 10).mapToObj(i -> userRepository.save(buildUser("user" + i))).toList();

    }

    @Test
    void userInDatabase() {
        List<User> savedUsers = userRepository.findAll();
        savedUsers.forEach(user -> System.out.println(user.getUsername() + " " + user.getEmail()));

        assertThat(savedUsers).hasSize(11);
    }

    @RepeatedTest(20)
    void raceConditionJoinEventTest() throws InterruptedException {
        LocalDateTime date = LocalDateTime.now().plusDays(7);
        CreateEventRequest createEventRequest = new CreateEventRequest();
        createEventRequest.setEventDateTime(date);
        createEventRequest.setTitle("volleyball 6 person");
        createEventRequest.setSport(SportType.VOLLEYBALL);
        createEventRequest.setLocation("ul. dluga102");
        createEventRequest.setMaxParticipants(4);

        EventResponse event = eventService.createEvent(createEventRequest, organizer, true);
        UUID eventId = event.getId();
        eventService.joinEvent(eventId, users.get(1));
        eventService.joinEvent(eventId, users.get(2));

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();

        Runnable join3 = () -> tryJoin(eventId, users.get(3), ready, start, successes);
        Runnable join4 = () -> tryJoin(eventId, users.get(4), ready, start, successes);

        CompletableFuture<Void> f1 = CompletableFuture.runAsync(join3, pool);
        CompletableFuture<Void> f2 = CompletableFuture.runAsync(join4, pool);
        try {
            ready.await();
            start.countDown();
            CompletableFuture.allOf(f1, f2).join();
        } finally {
            pool.shutdown();
        }

        assertThat(successes.get()).isEqualTo(1);

        List<ParticipantResponse> participants = eventService.getParticipants(eventId);
        participants.forEach(p -> System.out.println("Participant username: " + p.getUsername()));
        assertThat(participants).hasSize(4);
    }

    private void tryJoin(UUID eventId, User user, CountDownLatch ready, CountDownLatch start, AtomicInteger successes) {
        ready.countDown();
        try {
            start.await();
            eventService.joinEvent(eventId, user);
            successes.incrementAndGet();
        } catch (Exception e) {
            System.out.println(user.getUsername() + " failed: " + e);
        }
    }

    private User buildUser(String name) {
        return User.builder().username(name).email(name + "@test.com").password("encoded").role(Role.USER).build();
    }

    private Event buildEvent(User organizer, String title, SportType sportType, String location, LocalDateTime eventDateTime, int maxParticipants) {
        return Event.builder().title(title).sport(sportType).location(location).eventDateTime(eventDateTime)
                .maxParticipants(maxParticipants).organizer(organizer).build();
    }
}