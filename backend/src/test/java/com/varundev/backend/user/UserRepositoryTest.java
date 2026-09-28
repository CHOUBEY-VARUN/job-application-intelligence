package com.varundev.backend.user;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@DataJpaTest
@Testcontainers
class UserRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres
            = new PostgreSQLContainer("postgres:18");

    private final UserRepository userRepository;

    @Autowired
    UserRepositoryTest(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Test
    void shouldSaveAndRetrieveUser() {
        User user = new User();

        user.setEmail("test@example.com");
        user.setPasswordHash("hashed-password");

        User savedUser = userRepository.save(user);

        assertThat(savedUser.getId()).isNotNull();

        User retrievedUser = userRepository.findById(savedUser.getId())
                .orElseThrow();

        assertThat(retrievedUser.getEmail())
                .isEqualTo("test@example.com");

        assertThat(retrievedUser.getPasswordHash())
                .isEqualTo("hashed-password");

        assertThat(retrievedUser.getCreatedAt())
                .isNotNull();

        assertThat(retrievedUser.getUpdatedAt())
                .isNotNull();
    }

    @Test
    void shouldCheckIfUserExistsByEmail() {
        User user = new User();

        user.setEmail("exists@example.com");
        user.setPasswordHash("hashed-password");

        assertThat(userRepository.existsByEmail("exists@example.com"))
                .isFalse();

        userRepository.save(user);

        assertThat(userRepository.existsByEmail("exists@example.com"))
                .isTrue();
    }
}
