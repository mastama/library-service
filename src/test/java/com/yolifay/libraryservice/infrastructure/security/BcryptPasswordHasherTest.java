package com.yolifay.libraryservice.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BcryptPasswordHasherTest {

    private BcryptPasswordHasher hasher;

    @BeforeEach
    void setUp() {
        hasher = new BcryptPasswordHasher();
    }

    @Test
    void hash_shouldGenerateNonNullAndDifferentHashForSameInput() {
        // Arrange
        String rawPassword = "mypassword";

        // Act
        String hash1 = hasher.hash(rawPassword);
        String hash2 = hasher.hash(rawPassword);

        // Assert
        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2); // BCrypt menghasilkan hash berbeda tiap encode
        assertTrue(hash1.startsWith("$2")); // BCrypt hash biasanya mulai dengan $2
    }

    @Test
    void matches_shouldReturnTrueForValidPassword() {
        // Arrange
        String rawPassword = "mypassword";
        String hashed = hasher.hash(rawPassword);

        // Act
        boolean result = hasher.matches(rawPassword, hashed);

        // Assert
        assertTrue(result);
    }

    @Test
    void matches_shouldReturnFalseForInvalidPassword() {
        // Arrange
        String rawPassword = "mypassword";
        String wrongPassword = "wrongpass";
        String hashed = hasher.hash(rawPassword);

        // Act
        boolean result = hasher.matches(wrongPassword, hashed);

        // Assert
        assertFalse(result);
    }
}

