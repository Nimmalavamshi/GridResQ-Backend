package com.gridresq.backend.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin-setup")
public class AdminSetupController {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Value("${gridresq.admin.setup-key:}")
    private String setupKey;

    public AdminSetupController(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    public record AdminRequest(
            String setupKey,
            String fullName,
            String email,
            String password) {}

    @PostMapping
    public ResponseEntity<?> createAdmin(
            @RequestBody AdminRequest request) {

        if (setupKey == null || setupKey.isBlank()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "error",
                            "Admin setup is not enabled."
                    ));
        }

        if (request.setupKey() == null
                || !java.security.MessageDigest.isEqual(
                        setupKey.getBytes(
                                java.nio.charset.StandardCharsets.UTF_8),
                        request.setupKey().getBytes(
                                java.nio.charset.StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid setup key."));
        }

        if (request.fullName() == null || request.fullName().isBlank()
                || request.email() == null || request.email().isBlank()
                || request.password() == null
                || request.password().length() < 12) {
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "error",
                            "Provide a name, email, and password of at least 12 characters."
                    ));
        }

        Integer existingAdmins = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'",
                Integer.class
        );

        if (existingAdmins != null && existingAdmins > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            "An administrator already exists."
                    ));
        }

        String email = request.email().trim()
                .toLowerCase(java.util.Locale.ROOT);

        Integer existingEmail = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                email
        );

        if (existingEmail != null && existingEmail > 0) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "error",
                            "This email is already registered."
                    ));
        }

        jdbc.update(
                """
                INSERT INTO users
                    (full_name, email, password_hash, role, enabled)
                VALUES (?, ?, ?, 'ADMIN', TRUE)
                """,
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password())
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "message",
                        "Initial administrator created successfully."
                ));
    }
}