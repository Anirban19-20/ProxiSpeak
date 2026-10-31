package com.proxispeak.controller;

import com.proxispeak.dto.Dtos.*;
import com.proxispeak.entity.User;
import com.proxispeak.repository.UserRepository;
import com.proxispeak.security.JwtUtil;
import jakarta.validation.Valid;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtUtil jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }

    @PostMapping("/auth/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        String username = req.username().trim();
        String email = req.email().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already taken");
        }
        if (users.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        User saved;
        try {
            saved = users.save(new User(username, email, encoder.encode(req.password())));
        } catch (DuplicateKeyException e) { // two simultaneous registrations
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username or email already registered");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuth(saved));
    }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User user = users.findByUsername(req.username().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid username or password"));
        return toAuth(user);
    }

    @GetMapping("/auth/me")
    public UserResponse me(Authentication auth) {
        User u = users.findByUsername(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail());
    }

    private AuthResponse toAuth(User u) {
        return new AuthResponse(jwt.generate(u.getId(), u.getUsername()),
                new UserResponse(u.getId(), u.getUsername(), u.getEmail()));
    }
}
