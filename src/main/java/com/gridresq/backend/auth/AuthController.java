package com.gridresq.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository) {

        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    // REGISTER A NEW USER
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestBody(required = false) RegisterRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", "Registration details are required.")
            );
        }

        try {
            User user = authService.register(
                    request.fullName(),
                    request.email(),
                    request.password()
            );

            return ResponseEntity.ok(userResponse(user));

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(
                    Map.of("error", exception.getMessage())
            );

        } catch (DataAccessException exception) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "A database error occurred during registration.")
            );
        }
    }

    // LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody(required = false) LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        if (request == null
                || request.email() == null
                || request.email().isBlank()
                || request.password() == null
                || request.password().isEmpty()) {

            return ResponseEntity.badRequest().body(
                    Map.of("error", "Email and password are required.")
            );
        }

        String email = request.email()
                .trim()
                .toLowerCase(Locale.ROOT);

        try {
            // Authenticate using Spring Security.
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    email,
                                    request.password()
                            )
                    );

            // Create an authenticated security context.
            SecurityContext context =
                    SecurityContextHolder.createEmptyContext();

            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);

            // Prevent session fixation.
            HttpSession session = httpRequest.getSession(false);

            if (session != null) {
                session.invalidate();
            }

            httpRequest.getSession(true);

            // Persist the authenticated context in the HTTP session.
            securityContextRepository.saveContext(
                    context,
                    httpRequest,
                    httpResponse
            );

            // Return the authenticated user's public information.
            User user = authService.findByEmail(email);

            if (!user.enabled()) {
                SecurityContextHolder.clearContext();

                HttpSession currentSession =
                        httpRequest.getSession(false);

                if (currentSession != null) {
                    currentSession.invalidate();
                }

                return ResponseEntity.status(401).body(
                        Map.of("error", "This account is disabled.")
                );
            }

            return ResponseEntity.ok(userResponse(user));

        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();

            // Do not reveal whether the email or password was incorrect.
            return ResponseEntity.status(401).body(
                    Map.of(
                            "error",
                            "Invalid email or password, or the account is disabled."
                    )
            );

        } catch (DataAccessException exception) {
            SecurityContextHolder.clearContext();

            return ResponseEntity.internalServerError().body(
                    Map.of("error", "A database error occurred during login.")
            );
        }
    }

    // GET CURRENTLY AUTHENTICATED USER
    @GetMapping("/me")
    public ResponseEntity<?> currentUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserDetails)) {

            return ResponseEntity.status(401).body(
                    Map.of("error", "Please log in.")
            );
        }

        try {
            User user = authService.findByEmail(
                    authentication.getName()
            );

            if (!user.enabled()) {
                SecurityContextHolder.clearContext();

                return ResponseEntity.status(401).body(
                        Map.of("error", "This account is disabled.")
                );
            }

            return ResponseEntity.ok(userResponse(user));

        } catch (org.springframework.dao.EmptyResultDataAccessException exception) {
            SecurityContextHolder.clearContext();

            return ResponseEntity.status(401).body(
                    Map.of("error", "The account no longer exists.")
            );

        } catch (DataAccessException exception) {
            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Unable to retrieve account details.")
            );
        }
    }

    // LOGOUT
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        SecurityContextHolder.clearContext();

        HttpSession session = request.getSession(false);

        if (session != null) {
            session.invalidate();
        }

        return ResponseEntity.ok(
                Map.of("message", "Logged out successfully.")
        );
    }

    // PUBLIC USER RESPONSE — NEVER RETURN PASSWORD HASHES
    private Map<String, Object> userResponse(User user) {

        Map<String, Object> response = new LinkedHashMap<>();

        response.put("id", user.id());
        response.put("fullName", user.fullName());
        response.put("email", user.email());
        response.put("role", user.role());

        return response;
    }

    public record RegisterRequest(
            String fullName,
            String email,
            String password) {
    }

    public record LoginRequest(
            String email,
            String password) {
    }
}