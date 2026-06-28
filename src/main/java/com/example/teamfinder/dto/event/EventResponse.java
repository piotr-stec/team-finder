package com.example.teamfinder.dto.event;

import com.example.teamfinder.model.EventStatus;
import com.example.teamfinder.model.SportType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Full event details response")
public class EventResponse {

    private UUID id;
    private String title;
    private SportType sport;
    private String location;
    private LocalDateTime eventDateTime;
    private int maxParticipants;
    private int currentParticipants;
    private String description;
    private EventStatus status;
    private UUID organizerId;
    private String organizerUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
