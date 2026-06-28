package com.example.teamfinder.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Payload for updating user profile — all fields are optional")
public class UpdateUserRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, digits, and underscores")
    @Schema(example = "new_username")
    private String username;

    @Email(message = "Invalid email format")
    @Size(max = 100)
    @Schema(example = "new@example.com")
    private String email;

    @Size(min = 8, max = 100, message = "Password must be at least 8 characters")
    @Schema(description = "New password (leave empty to keep current)")
    private String password;
}
