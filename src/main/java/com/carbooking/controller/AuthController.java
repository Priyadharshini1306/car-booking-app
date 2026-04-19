package com.carbooking.controller;

import com.carbooking.dto.LoginRequest;
import com.carbooking.dto.RegisterRequest;
import com.carbooking.dto.JwtResponse;
import com.carbooking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ── Login ──
    @PostMapping("/login")
    public ResponseEntity<JwtResponse> login(
            @Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    // ── Register ──
    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    // ── Temporary: Create Admin directly ──
    // Open this URL in browser: http://localhost:8080/api/auth/create-admin
    // Delete this method after admin is created
    @GetMapping("/create-admin")
    public ResponseEntity<String> createAdmin() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Admin");
        req.setEmail("admin@carbooking.com");
        req.setPassword("admin123");
        req.setPhone("9999999999");
        try {
            authService.register(req);
            return ResponseEntity.ok(
                    "Admin created! Now run this SQL: " +
                            "UPDATE users SET role='ADMIN' " +
                            "WHERE email='admin@carbooking.com';");
        } catch (Exception e) {
            return ResponseEntity.ok(
                    "Error: " + e.getMessage() +
                            " - Delete admin from MySQL and try again.");
        }
    }
}