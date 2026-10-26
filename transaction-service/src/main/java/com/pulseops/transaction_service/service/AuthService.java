package com.pulseops.transaction_service.service;

import com.pulseops.transaction_service.domain.AppUser;
import com.pulseops.transaction_service.repository.AppUserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users; this.passwordEncoder = passwordEncoder;
    }
    public AppUser signup(String name, String email, String password) {
        var normalized = email.trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(normalized)) throw new IllegalStateException("Email is already registered");
        return users.save(new AppUser(name.trim(), normalized, passwordEncoder.encode(password)));
    }
    public AppUser authenticate(String email, String password) {
        return users.findByEmailIgnoreCase(email.trim()).filter(u -> passwordEncoder.matches(password, u.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
    }
}
