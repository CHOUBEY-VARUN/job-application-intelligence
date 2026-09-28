package com.varundev.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();

    @Test
    void VerifyPasswordEncoder() {
        String rawPassword = "secret123";

        String encodedPassword = passwordEncoder.encode(rawPassword);

        assertThat(encodedPassword)
                .isNotEqualTo(rawPassword);

        assertThat(passwordEncoder.matches(rawPassword, encodedPassword))
                .isTrue();

        assertThat(passwordEncoder.matches("wrongPassword", encodedPassword))
                .isFalse();
    }
}
