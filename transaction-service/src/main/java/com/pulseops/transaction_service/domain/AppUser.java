package com.pulseops.transaction_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.Instant;
import java.util.UUID;

@Entity(name = "AppUser")
@jakarta.persistence.Table(name = "app_user")
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(nullable = false, length = 120)
    private String name;
    @Column(nullable = false, unique = true, length = 320)
    private String email;
    @Column(nullable = false, length = 100)
    private String passwordHash;
    @Column(nullable = false)
    private Instant createdAt;

    protected AppUser() { }
    public AppUser(String name, String email, String passwordHash) {
        this.name = name; this.email = email; this.passwordHash = passwordHash; this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
}
