package com.example.teamfinder.dto.event;

import com.example.teamfinder.model.SportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "Payload for partially updating an event — all fields are optional")
public class UpdateEventRequest {

    @Size(min = 3, max = 100, message = "Title must be between 3 and 100 characters")
    private String title;

    private SportType sport;

    @Size(max = 200, message = "Location must be at most 200 characters")
    private String location;

    @Future(message = "Event date must be in the future")
    private LocalDateTime eventDateTime;

    @Min(value = 2, message = "At least 2 participants required")
    @Max(value = 100, message = "Maximum 100 participants allowed")
    private Integer maxParticipants;

    @Size(max = 1000, message = "Description must be at most 1000 characters")
    private String description;
}
