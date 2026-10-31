package com.proxispeak.dto;

import jakarta.validation.constraints.*;
import java.time.Instant;

public final class Dtos {
    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 40) @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                    message = "only letters, digits, _ . - allowed") String username,
            @NotBlank @Email @Size(max = 120) String email,
            @NotBlank @Size(min = 6, max = 100) String password) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record UserResponse(String id, String username, String email) {}

    public record AuthResponse(String token, UserResponse user) {}

    public record CreateRoomRequest(
            @NotBlank @Size(max = 80) String name,
            @Min(2) @Max(200) Integer maxUsers) {}

    public record RoomResponse(String roomCode, String name, String ownerUsername,
                               int maxUsers, long memberCount, boolean owner, Instant createdAt) {}

    public record SaveLayoutRequest(@Size(max = 80) String name,
                                    @NotBlank @Size(max = 500_000) String layoutJson) {}

    public record LayoutResponse(String roomCode, String name, String layoutJson, Instant updatedAt) {}

    
    public record RoomValidationResponse(boolean valid, boolean member, boolean full,
                                         String userId, String username,
                                         String roomCode, int maxUsers, long memberCount) {}

    public record ErrorResponse(int status, String error, String message) {}
}
