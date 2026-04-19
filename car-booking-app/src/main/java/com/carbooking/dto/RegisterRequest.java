package com.carbooking.dto;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    private String name;
    @Email @NotBlank
    private String email;
    @NotBlank @Size(min=6)
    private String password;
    private String phone;
}

