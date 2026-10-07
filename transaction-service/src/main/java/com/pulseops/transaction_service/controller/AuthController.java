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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final JwtEncoder jwtEncoder;
    private final String username;
    private final String password;

    public AuthController(JwtEncoder jwtEncoder,
            @Value("${app.demo.username}") String username,
            @Value("${app.demo.password}") String password) {
        this.jwtEncoder = jwtEncoder;
        this.username = username;
        this.password = password;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        if (!username.equals(request.username()) || !password.equals(request.password())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        var now = Instant.now();
        var claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(8, ChronoUnit.HOURS))
                .build();
        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        return new LoginResponse(jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).getTokenValue());
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) { }

    public record LoginResponse(String token) { }
}
