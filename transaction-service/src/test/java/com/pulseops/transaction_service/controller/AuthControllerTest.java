package com.pulseops.transaction_service.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pulseops.transaction_service.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.pulseops.transaction_service.service.AuthService;
import com.pulseops.transaction_service.domain.AppUser;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ImportAutoConfiguration({SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = {
        "app.jwt.secret=test-secret-that-is-long-enough-32",
        "app.demo.username=test-user",
        "app.demo.password=test-password",
        "app.demo.public-enabled=true"
})
class AuthControllerTest {
    @MockitoBean
    private AuthService authService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginReturnsJwtForValidCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-user\",\"password\":\"test-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void loginRejectsInvalidCredentials() throws Exception {
        when(authService.authenticate("test-user", "wrong")).thenThrow(new IllegalArgumentException("Invalid credentials"));
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"test-user\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicDemoLoginReturnsJwtWithoutAcceptingCredentials() throws Exception {
        mockMvc.perform(post("/api/v1/auth/demo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString());
    }

    @Test
    void signupReturnsJwt() throws Exception {
        when(authService.signup("Ada", "ada@example.com", "strong-password"))
                .thenReturn(new AppUser("Ada", "ada@example.com", "{bcrypt}hash"));
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"strong-password\",\"confirmPassword\":\"strong-password\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
    }

    @Test
    void signupRejectsInvalidEmail() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ada\",\"email\":\"not-an-email\",\"password\":\"strong-password\",\"confirmPassword\":\"strong-password\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Ada\",\"email\":\"ada@example.com\",\"password\":\"short\",\"confirmPassword\":\"short\"}"))
                .andExpect(status().isBadRequest());
    }
}
