package com.carbooking.dto;
import lombok.*;

@Data @AllArgsConstructor
public class JwtResponse {
    private String token;
    private String email;
    private String role;
    private Long   userId;
    private String name;
}