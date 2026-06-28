package com.example.teamfinder.service;

import com.example.teamfinder.dto.event.CreateEventRequest;
import com.example.teamfinder.dto.event.EventResponse;
import com.example.teamfinder.dto.event.UpdateEventRequest;
import com.example.teamfinder.exception.AlreadyRegisteredException;
import com.example.teamfinder.exception.BadRequestException;
import com.example.teamfinder.exception.EventFullException;
import com.example.teamfinder.exception.ForbiddenException;
import com.example.teamfinder.exception.ResourceNotFoundException;
import com.example.teamfinder.model.Event;
import com.example.teamfinder.model.EventStatus;
import com.example.teamfinder.model.SportType;
import com.example.teamfinder.model.User;
import com.example.teamfinder.model.Role;
import com.example.teamfinder.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService unit tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    private User organizer;
    private User participant;
    private Event event;

    @BeforeEach
    void setUp() {
        organizer = User.builder()
                .id(UUID.randomUUID())
                .username("organizer")
                .email("organizer@test.com")
                .password("encoded")
                .role(Role.USER)
                .enabled(true)
                .build();

        participant = User.builder()
                .id(UUID.randomUUID())
                .username("participant")
                .email("participant@test.com")
                .password("encoded")
                .role(Role.USER)
                .enabled(true)
                .build();

        event = Event.builder()
                .id(UUID.randomUUID())
                .title("Test Match")
                .sport(SportType.FOOTBALL)
                .location("Central Park")
                .eventDateTime(LocalDateTime.now().plusDays(7))
                .maxParticipants(10)
                .status(EventStatus.OPEN)
                .organizer(organizer)
                .participants(new HashSet<>())
                .build();
    }

    @Nested
    @DisplayName("createEvent()")
    class CreateEvent {

        @Test
        @DisplayName("Should create event and return response")
        void shouldCreateEvent() {
            CreateEventRequest request = new CreateEventRequest();
            request.setTitle("Sunday Match");
            request.setSport(SportType.BASKETBALL);
            request.setLocation("Gym A");
            request.setEventDateTime(LocalDateTime.now().plusDays(3));
            request.setMaxParticipants(8);
            request.setDescription("Friendly game");

            when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
                Event e = inv.getArgument(0);
                e.setCreatedAt(LocalDateTime.now());
                e.setUpdatedAt(LocalDateTime.now());
                return e;
            });

            EventResponse response = eventService.createEvent(request, organizer);

            assertThat(response.getTitle()).isEqualTo("Sunday Match");
            assertThat(response.getSport()).isEqualTo(SportType.BASKETBALL);
            assertThat(response.getStatus()).isEqualTo(EventStatus.OPEN);
            assertThat(response.getOrganizerUsername()).isEqualTo("organizer");
            verify(eventRepository).save(any(Event.class));
        }
    }

    @Nested
    @DisplayName("joinEvent()")
    class JoinEvent {

        @Test
        @DisplayName("Should allow participant to join open event")
        void shouldJoinEvent() {
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            EventResponse response = eventService.joinEvent(event.getId(), participant);

            assertThat(event.getParticipants()).contains(participant);
            verify(eventRepository).save(event);
        }

        @Test
        @DisplayName("Should throw EventFullException when event is full")
        void shouldThrowWhenEventFull() {
            // Fill the event to capacity
            for (int i = 0; i < 10; i++) {
                event.getParticipants().add(User.builder()
                        .id(UUID.randomUUID())
                        .username("user" + i)
                        .build());
            }
            event.updateStatus();
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.joinEvent(event.getId(), participant))
                    .isInstanceOf(EventFullException.class);
        }

        @Test
        @DisplayName("Should throw AlreadyRegisteredException when already joined")
        void shouldThrowWhenAlreadyJoined() {
            event.getParticipants().add(participant);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.joinEvent(event.getId(), participant))
                    .isInstanceOf(AlreadyRegisteredException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when organizer tries to join own event")
        void shouldThrowWhenOrganizerJoinsOwnEvent() {
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.joinEvent(event.getId(), organizer))
                    .isInstanceOf(BadRequestException.class);
        }

        @Test
        @DisplayName("Should throw BadRequestException when event is cancelled")
        void shouldThrowWhenEventCancelled() {
            event.setStatus(EventStatus.CANCELLED);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.joinEvent(event.getId(), participant))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("leaveEvent()")
    class LeaveEvent {

        @Test
        @DisplayName("Should allow participant to leave event")
        void shouldLeaveEvent() {
            event.getParticipants().add(participant);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            eventService.leaveEvent(event.getId(), participant);

            assertThat(event.getParticipants()).doesNotContain(participant);
        }

        @Test
        @DisplayName("Should throw BadRequestException when not registered")
        void shouldThrowWhenNotRegistered() {
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.leaveEvent(event.getId(), participant))
                    .isInstanceOf(BadRequestException.class);
        }
    }

    @Nested
    @DisplayName("updateEvent()")
    class UpdateEvent {

        @Test
        @DisplayName("Should update event when called by organizer")
        void shouldUpdateEvent() {
            UpdateEventRequest request = new UpdateEventRequest();
            request.setTitle("Updated Title");

            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            EventResponse response = eventService.updateEvent(event.getId(), request, organizer);

            assertThat(event.getTitle()).isEqualTo("Updated Title");
        }

        @Test
        @DisplayName("Should throw ForbiddenException when non-organizer tries to update")
        void shouldThrowForbiddenForNonOrganizer() {
            UpdateEventRequest request = new UpdateEventRequest();
            request.setTitle("Hack title");

            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.updateEvent(event.getId(), request, participant))
                    .isInstanceOf(ForbiddenException.class);
        }
    }

    @Nested
    @DisplayName("getEventById()")
    class GetEventById {

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown ID")
        void shouldThrowForUnknownId() {
            UUID unknownId = UUID.randomUUID();
            when(eventRepository.findById(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> eventService.getEventById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelEvent()")
    class CancelEvent {

        @Test
        @DisplayName("Should set status to CANCELLED")
        void shouldCancelEvent() {
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
            when(eventRepository.save(any(Event.class))).thenReturn(event);

            eventService.cancelEvent(event.getId(), organizer);

            assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
        }

        @Test
        @DisplayName("Should throw BadRequestException when already cancelled")
        void shouldThrowWhenAlreadyCancelled() {
            event.setStatus(EventStatus.CANCELLED);
            when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

            assertThatThrownBy(() -> eventService.cancelEvent(event.getId(), organizer))
                    .isInstanceOf(BadRequestException.class);
        }
    }
}
