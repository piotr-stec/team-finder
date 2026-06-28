package com.example.teamfinder.service;

import com.example.teamfinder.dto.auth.LoginRequest;
import com.example.teamfinder.dto.auth.RegisterRequest;
import com.example.teamfinder.dto.auth.AuthResponse;
import com.example.teamfinder.exception.BadRequestException;
import com.example.teamfinder.model.Role;
import com.example.teamfinder.model.User;
import com.example.teamfinder.repository.RefreshTokenRepository;
import com.example.teamfinder.repository.UserRepository;
import com.example.teamfinder.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService unit tests")
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationDays", 7L);
    }

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUser() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("new@example.com");
            request.setPassword("password123");

            User savedUser = User.builder()
                    .id(UUID.randomUUID())
                    .username("newuser")
                    .email("new@example.com")
                    .password("encoded")
                    .role(Role.USER)
                    .build();

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);
            when(jwtTokenProvider.generateAccessToken(any(User.class))).thenReturn("access-token");
            when(refreshTokenRepository.findByUser(any())).thenReturn(Optional.empty());
            when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.register(request);

            assertThat(response.getUsername()).isEqualTo("newuser");
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should throw BadRequestException when username is taken")
        void shouldThrowWhenUsernameTaken() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("taken");
            request.setEmail("email@test.com");
            request.setPassword("password123");

            when(userRepository.existsByUsername("taken")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("taken");
        }

        @Test
        @DisplayName("Should throw BadRequestException when email is taken")
        void shouldThrowWhenEmailTaken() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("newuser");
            request.setEmail("taken@test.com");
            request.setPassword("password123");

            when(userRepository.existsByUsername("newuser")).thenReturn(false);
            when(userRepository.existsByEmail("taken@test.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.register(request))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("taken");
        }
    }

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void shouldLogin() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("john");
            request.setPassword("password123");

            User user = User.builder()
                    .id(UUID.randomUUID())
                    .username("john")
                    .email("john@test.com")
                    .role(Role.USER)
                    .build();

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());

            when(authenticationManager.authenticate(any())).thenReturn(authToken);
            when(jwtTokenProvider.generateAccessToken(user)).thenReturn("jwt-token");
            when(refreshTokenRepository.findByUser(user)).thenReturn(Optional.empty());
            when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            AuthResponse response = authService.login(request);

            assertThat(response.getAccessToken()).isEqualTo("jwt-token");
            assertThat(response.getUsername()).isEqualTo("john");
        }

        @Test
        @DisplayName("Should propagate BadCredentialsException on wrong password")
        void shouldThrowOnBadCredentials() {
            LoginRequest request = new LoginRequest();
            request.setUsernameOrEmail("john");
            request.setPassword("wrong");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(request))
                    .isInstanceOf(BadCredentialsException.class);
        }
    }
}
