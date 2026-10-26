package com.pulseops.transaction_service.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pulseops.transaction_service.domain.AppUser;
import com.pulseops.transaction_service.repository.AppUserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class AuthServiceTest {
    @Test
    void signupStoresBcryptPassword() {
        var repository = org.mockito.Mockito.mock(AppUserRepository.class);
        var service = new AuthService(repository, new BCryptPasswordEncoder());
        when(repository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(false);
        when(repository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.signup("Ada", "ada@example.com", "strong-password");

        var captor = ArgumentCaptor.forClass(AppUser.class);
        verify(repository).save(captor.capture());
        assertTrue(new BCryptPasswordEncoder().matches("strong-password", captor.getValue().getPasswordHash()));
    }

    @Test
    void duplicateEmailIsRejected() {
        var repository = org.mockito.Mockito.mock(AppUserRepository.class);
        var service = new AuthService(repository, new BCryptPasswordEncoder());
        when(repository.existsByEmailIgnoreCase("ada@example.com")).thenReturn(true);
        org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
                () -> service.signup("Ada", "ada@example.com", "strong-password"));
    }
}
