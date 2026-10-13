package com.gridresq.backend.auth;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(
                    "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
            );

    public AuthService(
            JdbcTemplate jdbc,
            PasswordEncoder passwordEncoder) {

        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
    }

    // Register a new USER account.
    @Transactional
    public User register(
            String fullName,
            String email,
            String password) {

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required."
            );
        }

        if (fullName.trim().length() > 100) {
            throw new IllegalArgumentException(
                    "Full name must not exceed 100 characters."
            );
        }

        if (email == null ||
                !EMAIL_PATTERN.matcher(email.trim()).matches()) {

            throw new IllegalArgumentException(
                    "Enter a valid email address."
            );
        }

        String normalizedEmail =
                email.trim().toLowerCase(Locale.ROOT);

        if (normalizedEmail.length() > 150) {
            throw new IllegalArgumentException(
                    "Email must not exceed 150 characters."
            );
        }

        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException(
                    "Password must contain at least 8 characters."
            );
        }

        Integer existing = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?",
                Integer.class,
                normalizedEmail
        );

        if (existing != null && existing > 0) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        String passwordHash =
                passwordEncoder.encode(password);

        jdbc.update(
                """
                INSERT INTO users
                    (full_name, email, password_hash, role, enabled)
                VALUES
                    (?, ?, ?, 'USER', TRUE)
                """,
                fullName.trim(),
                normalizedEmail,
                passwordHash
        );

        return findByEmail(normalizedEmail);
    }

    // Find an account by email.
    public User findByEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required."
            );
        }

        return jdbc.queryForObject(
                """
                SELECT id, full_name, email, password_hash, role, enabled
                FROM users
                WHERE email = ?
                """,
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getBoolean("enabled")
                ),
                email.trim().toLowerCase(Locale.ROOT)
        );
    }

    // Retrieve registered accounts without returning password hashes.
    public List<UserSummary> getAllUsers() {

        return jdbc.query(
                """
                SELECT id, full_name, email, role, enabled, created_at
                FROM users
                ORDER BY id DESC
                """,
                (rs, rowNum) -> new UserSummary(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getBoolean("enabled"),
                        rs.getTimestamp("created_at") == null
                                ? null
                                : rs.getTimestamp("created_at")
                                        .toLocalDateTime()
                )
        );
    }

    // Count registered accounts.
    public long countUsers() {

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users",
                Long.class
        );

        return count == null ? 0 : count;
    }

    // Count administrator accounts.
    public long countAdministrators() {

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'",
                Long.class
        );

        return count == null ? 0 : count;
    }

    // Count enabled accounts.
    public long countEnabledUsers() {

        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM users WHERE enabled = TRUE",
                Long.class
        );

        return count == null ? 0 : count;
    }

    // Enable or disable an account.
    @Transactional
    public void setUserEnabled(
            long userId,
            boolean enabled) {

        int updated = jdbc.update(
                "UPDATE users SET enabled = ? WHERE id = ?",
                enabled,
                userId
        );

        if (updated == 0) {
            throw new IllegalArgumentException(
                    "User account was not found."
            );
        }
    }

    // Change a user's role.
    @Transactional
    public void updateUserRole(
            long userId,
            String role) {

        if (!"USER".equals(role) && !"ADMIN".equals(role)) {
            throw new IllegalArgumentException(
                    "Role must be USER or ADMIN."
            );
        }

        int updated = jdbc.update(
                "UPDATE users SET role = ? WHERE id = ?",
                role,
                userId
        );

        if (updated == 0) {
            throw new IllegalArgumentException(
                    "User account was not found."
            );
        }
    }
    public boolean checkAdminPassword(String rawPassword) {
    User user = findByEmail("rajunimmala90@gmail.com");
    return passwordEncoder.matches(rawPassword, user.passwordHash());
}

    // Public account information.
    public record UserSummary(
            Long id,
            String fullName,
            String email,
            String role,
            boolean enabled,
            java.time.LocalDateTime createdAt) {
    }
}