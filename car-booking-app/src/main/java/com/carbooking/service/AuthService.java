package com.carbooking.service;

import com.carbooking.dto.*;
import com.carbooking.entity.User;
import com.carbooking.repository.UserRepository;
import com.carbooking.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository    userRepo;
    private final PasswordEncoder   passwordEncoder;
    private final JwtUtil           jwtUtil;
    private final AuthenticationManager authManager;

    // =============================================
    // LOGIN
    // =============================================
    // HOW THIS WORKS (simple explanation):
    // 1. authManager.authenticate() calls Spring
    //    Security which loads the user from DB via
    //    UserDetailsServiceImpl.loadUserByUsername()
    // 2. Spring Security then uses PasswordEncoder
    //    to compare the raw password you typed with
    //    the BCrypt hash stored in the DB.
    // 3. If they match → authentication succeeds.
    // 4. If they don't → BadCredentialsException.
    //
    // WHY IT WAS FAILING:
    // The JWT_SECRET "mySuperSecretKey" is 16 chars.
    // HMAC-SHA256 requires a key of at least 256 bits
    // = 32 bytes = 32 characters minimum.
    // When the secret is too short, jjwt either
    // throws an error or generates an invalid token.
    // The invalid token then fails on every subsequent
    // request, making it look like login is broken
    // even when password comparison is correct.
    // FIX: Use a 64-character JWT_SECRET.
    // =============================================
    public JwtResponse login(LoginRequest req) {
        // This will throw BadCredentialsException
        // if email or password is wrong
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                req.getEmail(),
                req.getPassword()
            )
        );

        User user = userRepo
            .findByEmail(req.getEmail())
            .orElseThrow(() ->
                new RuntimeException("User not found"));

        String token = jwtUtil.generateToken(
            user.getEmail(),
            user.getRole().name()
        );

        return new JwtResponse(
            token,
            user.getEmail(),
            user.getRole().name(),
            user.getId(),
            user.getName()
        );
    }

    // =============================================
    // REGISTER
    // =============================================
    // HOW THIS WORKS:
    // 1. Check email is not already used.
    // 2. Encode the password with BCrypt BEFORE
    //    saving to the database.
    //    BCrypt turns "abc123" into something like
    //    "$2a$10$xyz..." which is stored in the DB.
    // 3. When login happens, Spring Security runs
    //    passwordEncoder.matches("abc123", "$2a$10$...")
    //    which returns true only if they match.
    //
    // IMPORTANT: NEVER store plain-text passwords.
    // Always encode BEFORE save. Always use
    // passwordEncoder.matches() to compare — never
    // compare raw strings directly.
    // =============================================
    public String register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException(
                "Email already registered"
            );
        }

        User user = User.builder()
            .name(req.getName())
            .email(req.getEmail())
            // BCrypt encode the password here
            .password(passwordEncoder.encode(
                req.getPassword()
            ))
            .phone(req.getPhone())
            .role(User.Role.USER)
            .build();

        userRepo.save(user);
        return "User registered successfully";
    }
}