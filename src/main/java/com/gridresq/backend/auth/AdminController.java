package com.gridresq.backend.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AuthService authService;
    private final JdbcTemplate jdbc;

    public AdminController(
            AuthService authService,
            JdbcTemplate jdbc) {

        this.authService = authService;
        this.jdbc = jdbc;
    }

    // =========================
    // ADMIN DASHBOARD STATISTICS
    // =========================

    @GetMapping("/stats")
    public ResponseEntity<?> getDashboardStats() {

        Map<String, Object> stats = new LinkedHashMap<>();

        stats.put("totalUsers", authService.countUsers());
        stats.put(
                "totalAdministrators",
                authService.countAdministrators()
        );
        stats.put(
                "enabledUsers",
                authService.countEnabledUsers()
        );

        Long totalSimulations = jdbc.queryForObject(
                "SELECT COUNT(*) FROM simulation_history",
                Long.class
        );

        stats.put(
                "totalSimulations",
                totalSimulations == null ? 0 : totalSimulations
        );

        return ResponseEntity.ok(stats);
    }

    // =========================
    // GET ALL USERS
    // =========================

    @GetMapping("/users")
    public ResponseEntity<?> getUsers() {

        return ResponseEntity.ok(
                authService.getAllUsers()
        );
    }

    // =========================
    // ENABLE OR DISABLE A USER
    // =========================

    @PatchMapping("/users/{userId}/status")
    @Transactional
    public ResponseEntity<?> updateUserStatus(
            @PathVariable long userId,
            @RequestBody StatusRequest request,
            Authentication authentication) {

        if (userId <= 0) {
            return error(
                    400,
                    "Invalid user ID."
            );
        }

        if (request == null || request.enabled() == null) {
            return error(
                    400,
                    "The enabled field is required."
            );
        }

        try {
            User administrator =
                    authService.findByEmail(authentication.getName());

            User target = findUserById(userId);

            if (target == null) {
                return error(
                        404,
                        "User account was not found."
                );
            }

            // Administrators cannot disable their own account.
            if (administrator.id().equals(target.id())
                    && !request.enabled()) {

                return error(
                        400,
                        "You cannot disable your own account."
                );
            }

            // Prevent disabling the last enabled administrator.
            if ("ADMIN".equals(target.role())
                    && target.enabled()
                    && !request.enabled()) {

                long remainingAdmins =
                        countOtherEnabledAdministrators(userId);

                if (remainingAdmins == 0) {
                    return error(
                            400,
                            "You cannot disable the last enabled administrator."
                    );
                }
            }

            authService.setUserEnabled(
                    userId,
                    request.enabled()
            );

            Map<String, Object> result =
                    new LinkedHashMap<>();

            result.put(
                    "message",
                    request.enabled()
                            ? "User account enabled successfully."
                            : "User account disabled successfully."
            );

            result.put("userId", userId);
            result.put("enabled", request.enabled());

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException exception) {
            return error(
                    400,
                    exception.getMessage()
            );
        }
    }

    // =========================
    // CHANGE USER ROLE
    // =========================

    @PatchMapping("/users/{userId}/role")
    @Transactional
    public ResponseEntity<?> updateUserRole(
            @PathVariable long userId,
            @RequestBody RoleRequest request,
            Authentication authentication) {

        if (userId <= 0) {
            return error(
                    400,
                    "Invalid user ID."
            );
        }

        if (request == null
                || request.role() == null
                || request.role().isBlank()) {

            return error(
                    400,
                    "The role field is required."
            );
        }

        String newRole = request.role()
                .trim()
                .toUpperCase(java.util.Locale.ROOT);

        if (!"USER".equals(newRole)
                && !"ADMIN".equals(newRole)) {

            return error(
                    400,
                    "Role must be USER or ADMIN."
            );
        }

        try {
            User administrator =
                    authService.findByEmail(authentication.getName());

            User target = findUserById(userId);

            if (target == null) {
                return error(
                        404,
                        "User account was not found."
                );
            }

            // An administrator cannot demote themselves.
            if (administrator.id().equals(target.id())
                    && !"ADMIN".equals(newRole)) {

                return error(
                        400,
                        "You cannot remove your own administrator role."
                );
            }

            // Prevent demoting the last enabled administrator.
            if ("ADMIN".equals(target.role())
                    && target.enabled()
                    && "USER".equals(newRole)) {

                long remainingAdmins =
                        countOtherEnabledAdministrators(userId);

                if (remainingAdmins == 0) {
                    return error(
                            400,
                            "You cannot demote the last enabled administrator."
                    );
                }
            }

            authService.updateUserRole(
                    userId,
                    newRole
            );

            Map<String, Object> result =
                    new LinkedHashMap<>();

            result.put(
                    "message",
                    "User role updated successfully."
            );

            result.put("userId", userId);
            result.put("role", newRole);

            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException exception) {
            return error(
                    400,
                    exception.getMessage()
            );
        }
    }

    // =========================
    // FIND USER BY ID
    // =========================

    private User findUserById(long userId) {

        var users = jdbc.query(
                """
                SELECT id, full_name, email,
                       password_hash, role, enabled
                FROM users
                WHERE id = ?
                """,
                (rs, rowNum) -> new User(
                        rs.getLong("id"),
                        rs.getString("full_name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getBoolean("enabled")
                ),
                userId
        );

        return users.isEmpty() ? null : users.get(0);
    }

    // =========================
    // COUNT OTHER ENABLED ADMINS
    // =========================

    private long countOtherEnabledAdministrators(
            long excludedUserId) {

        Long count = jdbc.queryForObject(
                """
                SELECT COUNT(*)
                FROM users
                WHERE role = 'ADMIN'
                  AND enabled = TRUE
                  AND id <> ?
                """,
                Long.class,
                excludedUserId
        );

        return count == null ? 0 : count;
    }

    // =========================
    // STANDARD ERROR RESPONSE
    // =========================

    private ResponseEntity<Map<String, String>> error(
            int status,
            String message) {

        return ResponseEntity
                .status(status)
                .body(Map.of("error", message));
    }

    public record StatusRequest(Boolean enabled) {
    }

    public record RoleRequest(String role) {
    }
}