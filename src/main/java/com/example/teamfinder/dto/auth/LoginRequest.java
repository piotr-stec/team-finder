package com.example.teamfinder.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Login request payload")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @Schema(example = "john_doe", description = "Username or email address")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    @Schema(example = "SecurePass1!")
    private String password;
}
