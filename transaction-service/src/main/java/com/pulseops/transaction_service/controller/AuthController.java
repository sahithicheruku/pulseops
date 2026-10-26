package com.pulseops.transaction_service.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import com.pulseops.transaction_service.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final JwtEncoder jwtEncoder;
    private final String username;
    private final String password;
    private final AuthService authService;

    public AuthController(JwtEncoder jwtEncoder,
            @Value("${app.demo.username}") String username,
            @Value("${app.demo.password}") String password, AuthService authService) {
        this.jwtEncoder = jwtEncoder;
        this.username = username;
        this.password = password;
        this.authService = authService;
    }

    @PostMapping("/signup")
    public LoginResponse signup(@Valid @RequestBody SignupRequest request) {
        if (!request.password().equals(request.confirmPassword())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
        try {
            var user = authService.signup(request.name(), request.email(), request.password());
            return token(user.getEmail());
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage());
        }
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (username.equals(request.username()) && password.equals(request.password())) return token(username);
        try {
            var user = authService.authenticate(request.username(), request.password());
            if (user == null) throw new IllegalArgumentException("Invalid credentials");
            return token(user.getEmail());
        }
        catch (IllegalArgumentException exception) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"); }
    }
    private LoginResponse token(String subject) {
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(8, ChronoUnit.HOURS))
                .build();
        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return new LoginResponse(jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue());
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }
    public record SignupRequest(@NotBlank @Size(max = 120) String name, @NotBlank @Email String email,
            @NotBlank @Size(min = 8, max = 72) String password, @NotBlank String confirmPassword) { }

    public record LoginResponse(String token) { }
}
