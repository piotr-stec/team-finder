package com.example.teamfinder.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Participant info in an event")
public class ParticipantResponse {
    private UUID id;
    private String username;
}
