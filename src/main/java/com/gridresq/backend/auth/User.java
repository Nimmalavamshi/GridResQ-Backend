package com.gridresq.backend.auth;

public record User(
        Long id,
        String fullName,
        String email,
        String passwordHash,
        String role,
        boolean enabled
) {}