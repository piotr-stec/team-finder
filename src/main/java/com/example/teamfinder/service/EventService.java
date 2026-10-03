package com.example.teamfinder.service;

import com.example.teamfinder.dto.event.CreateEventRequest;
import com.example.teamfinder.dto.event.EventResponse;
import com.example.teamfinder.dto.event.UpdateEventRequest;
import com.example.teamfinder.dto.user.ParticipantResponse;
import com.example.teamfinder.exception.AlreadyRegisteredException;
import com.example.teamfinder.exception.BadRequestException;
import com.example.teamfinder.exception.EventFullException;
import com.example.teamfinder.exception.ForbiddenException;
import com.example.teamfinder.exception.ResourceNotFoundException;
import com.example.teamfinder.model.Event;
import com.example.teamfinder.model.EventStatus;
import com.example.teamfinder.model.SportType;
import com.example.teamfinder.model.User;
import com.example.teamfinder.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    // ─── Read operations ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<EventResponse> getEvents(SportType sport, LocalDateTime from,
                                         LocalDateTime to, Pageable pageable) {
        return eventRepository.findAll(EventRepository.withFilters(sport, from, to), pageable)
                .map(this::toEventResponse);
    }

    @Transactional(readOnly = true)
    public EventResponse getEventById(UUID id) {
        Event event = findEventById(id);
        return toEventResponse(event);
    }

    @Transactional(readOnly = true)
    public List<ParticipantResponse> getParticipants(UUID eventId) {
        Event event = findEventById(eventId);
        return event.getParticipants().stream()
                .map(u -> ParticipantResponse.builder()
                        .id(u.getId())
                        .username(u.getUsername())
                        .build())
                .toList();
    }

    // ─── Write operations ───────────────────────────────────────────────────────

    @Transactional
    public EventResponse createEvent(CreateEventRequest request, User organizer, boolean joinOrganizer) {
        Event event = Event.builder()
                .title(request.getTitle())
                .sport(request.getSport())
                .location(request.getLocation())
                .eventDateTime(request.getEventDateTime())
                .maxParticipants(request.getMaxParticipants())
                .description(request.getDescription())
                .organizer(organizer)
                .status(EventStatus.OPEN)
                .build();

        if (joinOrganizer){
            event.getParticipants().add(organizer);
        }

        event = eventRepository.save(event);
        log.info("Event created: {} by user: {}", event.getId(), organizer.getUsername());
        return toEventResponse(event);
    }

    @Transactional
    public EventResponse updateEvent(UUID id, UpdateEventRequest request, User currentUser) {
        Event event = findEventByIdForUpdate(id);
        assertIsOrganizer(event, currentUser);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled event");
        }

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getSport() != null) event.setSport(request.getSport());
        if (request.getLocation() != null) event.setLocation(request.getLocation());
        if (request.getEventDateTime() != null) event.setEventDateTime(request.getEventDateTime());
        if (request.getMaxParticipants() != null) {
            if (request.getMaxParticipants() < event.getCurrentParticipants()) {
                throw new BadRequestException(
                        "Cannot set max participants below current participant count ("
                        + event.getCurrentParticipants() + ")");
            }
            event.setMaxParticipants(request.getMaxParticipants());
        }
        if (request.getDescription() != null) event.setDescription(request.getDescription());

        event.updateStatus();
        event = eventRepository.save(event);
        log.info("Event updated: {} by user: {}", id, currentUser.getUsername());
        return toEventResponse(event);
    }

    @Transactional
    public void cancelEvent(UUID id, User currentUser) {
        Event event = findEventByIdForUpdate(id);
        assertIsOrganizer(event, currentUser);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Event is already cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);
        log.info("Event cancelled: {} by user: {}", id, currentUser.getUsername());
    }

    @Transactional
    public void deleteEvent(UUID id, User currentUser) {
        Event event = findEventByIdForUpdate(id);
        // Admin can delete any event; organizer can delete their own
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            assertIsOrganizer(event, currentUser);
        }
        eventRepository.delete(event);
        log.info("Event deleted: {} by user: {}", id, currentUser.getUsername());
    }

    // ─── Participant operations ─────────────────────────────────────────────────

    @Transactional
    public EventResponse joinEvent(UUID eventId, User user) {
        Event event = findEventByIdForUpdate(eventId);

        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot join a cancelled event");
        }
        if (event.getStatus() == EventStatus.FULL || event.isFull()) {
            throw new EventFullException();
        }

        if (event.getParticipants().contains(user)) {
            throw new AlreadyRegisteredException("You are already registered for this event");
        }

        event.getParticipants().add(user);
        event.updateStatus();
        event = eventRepository.save(event);
        log.info("User {} joined event {}", user.getUsername(), eventId);
        return toEventResponse(event);
    }

    @Transactional
    public EventResponse leaveEvent(UUID eventId, User user) {
        Event event = findEventByIdForUpdate(eventId);

        if (!event.getParticipants().contains(user)) {
            throw new BadRequestException("You are not registered for this event");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new BadRequestException("Cannot leave a cancelled event");
        }

        event.getParticipants().remove(user);
        event.updateStatus();
        event = eventRepository.save(event);
        log.info("User {} left event {}", user.getUsername(), eventId);
        return toEventResponse(event);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────────

    private Event findEventById(UUID id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    private Event findEventByIdForUpdate(UUID id) {
        return eventRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));
    }

    private void assertIsOrganizer(Event event, User user) {
        if (!event.getOrganizer().getId().equals(user.getId())) {
            throw new ForbiddenException("Only the event organizer can perform this action");
        }
    }

    private EventResponse toEventResponse(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .sport(event.getSport())
                .location(event.getLocation())
                .eventDateTime(event.getEventDateTime())
                .maxParticipants(event.getMaxParticipants())
                .currentParticipants(event.getCurrentParticipants())
                .description(event.getDescription())
                .status(event.getStatus())
                .organizerId(event.getOrganizer().getId())
                .organizerUsername(event.getOrganizer().getUsername())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}
