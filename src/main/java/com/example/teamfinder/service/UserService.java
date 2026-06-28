package com.example.teamfinder.service;

import com.example.teamfinder.dto.user.UpdateUserRequest;
import com.example.teamfinder.dto.user.UserResponse;
import com.example.teamfinder.exception.BadRequestException;
import com.example.teamfinder.exception.ResourceNotFoundException;
import com.example.teamfinder.model.User;
import com.example.teamfinder.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserResponse getProfile(User user) {
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(User user, UpdateUserRequest request) {
        if (request.getUsername() != null &&
                !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new BadRequestException(
                        "Username '" + request.getUsername() + "' is already taken");
            }
            user.setUsername(request.getUsername());
        }

        if (request.getEmail() != null &&
                !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException(
                        "Email '" + request.getEmail() + "' is already registered");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getUsername());
        return toUserResponse(user);
    }

    // ─── Admin operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toUserResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        return toUserResponse(user);
    }

    @Transactional
    public UserResponse toggleUserEnabled(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        user.setEnabled(!user.isEnabled());
        user = userRepository.save(user);
        log.info("User {} enabled status toggled to: {}", user.getUsername(), user.isEnabled());
        return toUserResponse(user);
    }

    @Transactional
    public void deleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
        userRepository.delete(user);
        log.info("User deleted: {}", id);
    }

    // ─── Mapper ─────────────────────────────────────────────────────────────────

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
