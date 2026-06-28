package com.example.teamfinder.dto.auth;

import com.example.teamfinder.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@Schema(description = "Authentication response containing JWT tokens and user info")
public class AuthResponse {

    @Schema(description = "Short-lived JWT access token (15 minutes)")
    private String accessToken;

    @Schema(description = "Long-lived refresh token (7 days)")
    private String refreshToken;

    @Builder.Default
    @Schema(example = "Bearer")
    private String tokenType = "Bearer";

    private UUID userId;
    private String username;
    private String email;
    private Role role;
}
