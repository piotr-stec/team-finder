package com.example.teamfinder.controller;

import com.example.teamfinder.dto.event.CreateEventRequest;
import com.example.teamfinder.dto.event.EventResponse;
import com.example.teamfinder.dto.event.UpdateEventRequest;
import com.example.teamfinder.dto.user.ParticipantResponse;
import com.example.teamfinder.model.SportType;
import com.example.teamfinder.model.User;
import com.example.teamfinder.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Create and manage sports events")
public class EventController {

    private final EventService eventService;

    // ─── Public endpoints ───────────────────────────────────────────────────────

    @GetMapping
    @Operation(summary = "List all events with optional filtering by sport and date range")
    public ResponseEntity<Page<EventResponse>> getEvents(
            @Parameter(description = "Filter by sport type")
            @RequestParam(required = false) SportType sport,

            @Parameter(description = "Filter events from this date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,

            @Parameter(description = "Filter events up to this date (ISO format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,

            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // Build Pageable manually to avoid Spring MVC translating Direction as a 'string' sort column
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "eventDateTime"));
        return ResponseEntity.ok(eventService.getEvents(sport, from, to, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event details by ID")
    public ResponseEntity<EventResponse> getEventById(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getEventById(id));
    }

    @GetMapping("/{id}/participants")
    @Operation(summary = "Get list of participants for an event")
    public ResponseEntity<List<ParticipantResponse>> getParticipants(@PathVariable UUID id) {
        return ResponseEntity.ok(eventService.getParticipants(id));
    }

    // ─── Authenticated endpoints ────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new sports event")
    public ResponseEntity<EventResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(eventService.createEvent(request, currentUser));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an event (organizer only)")
    public ResponseEntity<EventResponse> updateEvent(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateEventRequest request,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(eventService.updateEvent(id, request, currentUser));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(summary = "Cancel an event (organizer only)")
    public ResponseEntity<Void> cancelEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        eventService.cancelEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an event (organizer or admin)")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        eventService.deleteEvent(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    // ─── Participant endpoints ──────────────────────────────────────────────────

    @PostMapping("/{id}/join")
    @Operation(summary = "Join an event as a participant")
    public ResponseEntity<EventResponse> joinEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(eventService.joinEvent(id, currentUser));
    }

    @DeleteMapping("/{id}/leave")
    @Operation(summary = "Leave an event")
    public ResponseEntity<EventResponse> leaveEvent(
            @PathVariable UUID id,
            @AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(eventService.leaveEvent(id, currentUser));
    }
}
