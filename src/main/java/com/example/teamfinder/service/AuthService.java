package com.example.teamfinder.service;

import com.example.teamfinder.dto.auth.AuthResponse;
import com.example.teamfinder.dto.auth.LoginRequest;
import com.example.teamfinder.dto.auth.RefreshTokenRequest;
import com.example.teamfinder.dto.auth.RegisterRequest;
import com.example.teamfinder.exception.BadRequestException;
import com.example.teamfinder.exception.TokenException;
import com.example.teamfinder.model.RefreshToken;
import com.example.teamfinder.model.Role;
import com.example.teamfinder.model.User;
import com.example.teamfinder.repository.RefreshTokenRepository;
import com.example.teamfinder.repository.UserRepository;
import com.example.teamfinder.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.refresh-token-expiration-days}")
    private long refreshTokenExpirationDays;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username '" + request.getUsername() + "' is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email '" + request.getEmail() + "' is already registered");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .enabled(true)
                .build();

        user = userRepository.save(user);
        log.info("New user registered: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsernameOrEmail(),
                        request.getPassword()));

        User user = (User) authentication.getPrincipal();
        log.info("User logged in: {}", user.getUsername());

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new TokenException("Invalid refresh token"));

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new TokenException("Refresh token has expired. Please log in again.");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshTokenValue = generateRefreshTokenValue();

        refreshToken.setToken(newRefreshTokenValue);
        refreshToken.setExpiryDate(Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        log.info("Tokens refreshed for user: {}", user.getUsername());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshTokenValue)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    @Transactional
    public void logout(User user) {
        refreshTokenRepository.deleteByUser(user);
        log.info("User logged out: {}", user.getUsername());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = createOrUpdateRefreshToken(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenValue)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }

    private String createOrUpdateRefreshToken(User user) {
        String tokenValue = generateRefreshTokenValue();
        Instant expiryDate = Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS);

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseGet(() -> RefreshToken.builder().user(user).build());

        refreshToken.setToken(tokenValue);
        refreshToken.setExpiryDate(expiryDate);
        refreshTokenRepository.save(refreshToken);

        return tokenValue;
    }

    private String generateRefreshTokenValue() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }
}
