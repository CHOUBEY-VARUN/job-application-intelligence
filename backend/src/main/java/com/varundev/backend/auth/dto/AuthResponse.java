package com.varundev.backend.auth.dto;

import java.util.UUID;

public class AuthResponse {

    private final UUID id;
    private final String email;

    public AuthResponse(UUID id, String email) {
        this.id = id;
        this.email = email;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }
}