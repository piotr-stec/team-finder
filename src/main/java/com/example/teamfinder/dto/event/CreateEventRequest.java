package com.example.teamfinder.dto.event;

import com.example.teamfinder.model.SportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Payload for creating a new sports event")
public class CreateEventRequest {

    @NotBlank(message = "Title is required")
    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    @Schema(example = "Sunday Football Match")
    private String title;

    @NotNull(message = "Sport type is required")
    @Schema(example = "FOOTBALL")
    private SportType sport;

    @NotBlank(message = "Location is required")
    @Size(max = 200, message = "Location must be at most 200 characters")
    @Schema(example = "Central Park, Field 3")
    private String location;

    @NotNull(message = "Event date and time is required")
    @Future(message = "Event date must be in the future")
    @Schema(example = "2025-08-15T10:00:00", description = "ISO 8601 date-time")
    private LocalDateTime eventDateTime;

    @Min(value = 2, message = "At least 2 participants required")
    @Max(value = 100, message = "Maximum 100 participants allowed")
    @Schema(example = "10")
    private int maxParticipants;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    @Schema(example = "Casual friendly match, all skill levels welcome!")
    private String description;
}
