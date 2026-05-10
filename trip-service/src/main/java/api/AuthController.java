package org.example.taxi.trip.api;

import org.example.taxi.trip.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final JwtService jwtService;
    private final String username;
    private final String password;

    public AuthController(JwtService jwtService,
                          @Value("${app.auth.username:admin}") String username,
                          @Value("${app.auth.password:admin}") String password) {
        this.jwtService = jwtService;
        this.username = username;
        this.password = password;
    }

    @PostMapping("/token")
    public AuthResponse token(@RequestBody AuthRequest request) {
        if (!username.equals(request.username()) || !password.equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new AuthResponse(jwtService.generateToken(request.username()));
    }

    public record AuthRequest(String username, String password) {}
    public record AuthResponse(String token) {}
}
