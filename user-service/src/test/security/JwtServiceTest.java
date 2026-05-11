package security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    @Test
    void generateAndValidateToken() {
        JwtService jwtService = new JwtService("the-secret-key-is-32-characters-123", 60_000);

        String token = jwtService.generateToken("admin");
        String subject = jwtService.validateAndGetSubject(token);

        assertEquals("admin", subject);
    }

    @Test
    void fakeTokenIsRejected() {
        JwtService jwtService = new JwtService("the-secret-key-is-32-characters-123", 60_000);

        String realToken = jwtService.generateToken("admin");

        String fakeToken = realToken.substring(0, realToken.length() - 1) + "X";

        assertThrows(Exception.class, () -> {
            jwtService.validateAndGetSubject(fakeToken);
        });
    }
}
