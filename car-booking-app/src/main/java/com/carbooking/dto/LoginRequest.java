package com.carbooking.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LoginRequest {
    @Email @NotBlank
    private String email;
    @NotBlank @Size(min=6)
    private String password;
}

