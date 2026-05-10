package org.example.taxi.user.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JwtServiceTest {

    @Test
    void generateAndValidateToken() {
        JwtService jwtService = new JwtService("the-secret-key-is-32-characters-123", 60_000);

        String token = jwtService.generateToken("admin");
        String subject = jwtService.validateAndGetSubject(token);

        assertEquals("admin", subject);
    }
}
