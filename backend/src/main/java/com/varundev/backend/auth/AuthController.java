package com.varundev.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.varundev.backend.auth.dto.AuthResponse;
import com.varundev.backend.auth.dto.LoginRequest;
import com.varundev.backend.auth.dto.RegisterRequest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import jakarta.validation.Valid;

@RestController
public class AuthController {

    private final AuthService authService;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(
            AuthService authService,
            SecurityContextRepository securityContextRepository) {

        this.authService = authService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/api/auth/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthResponse response = authService.register(
                request.getEmail(),
                request.getPassword());

        securityContextRepository.saveContext(
                SecurityContextHolder.getContext(),
                httpRequest,
                httpResponse);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {

        AuthResponse response = authService.login(
                request.getEmail(),
                request.getPassword());

        securityContextRepository.saveContext(
                SecurityContextHolder.getContext(),
                httpRequest,
                httpResponse);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/auth/me")
    public ResponseEntity<AuthResponse> me() {
        AuthResponse response = authService.getCurrentUser();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/api/auth/csrf")
    public ResponseEntity<CsrfToken> csrf(CsrfToken csrfToken) {
        return ResponseEntity.ok(csrfToken);
    }

}
