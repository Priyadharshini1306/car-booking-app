package com.carbooking.config;

import com.carbooking.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s ->
                        s.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // ── Public Auth ──
                        .requestMatchers(
                                "/api/auth/**")
                        .permitAll()

                        // ── Public Car ──
                        .requestMatchers(
                                "/api/cars/public/**")
                        .permitAll()

                        // ── Public Feedback ──
                        .requestMatchers(
                                "/api/feedback/public/**")
                        .permitAll()

                        // ── Driver (all public) ──
                        .requestMatchers(
                                "/api/driver/**")
                        .permitAll()

                        // ── Actuator ──
                        .requestMatchers(
                                "/actuator/**")
                        .permitAll()

                        // ── Static HTML Pages ──
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/login.html",
                                "/cars.html",
                                "/booking.html",
                                "/dashboard.html",
                                "/feedback.html",
                                "/chat.html",
                                "/admin.html",
                                "/driver.html",
                                "/*.html"
                        ).permitAll()

                        // ── Static Resources ──
                        .requestMatchers(
                                "/css/**",
                                "/js/**",
                                "/img/**",
                                "/*.css",
                                "/*.js",
                                "/*.png",
                                "/*.jpg",
                                "/*.ico",
                                "/favicon.ico",
                                "/favicon.png"
                        ).permitAll()

                        // ── Admin Only ──
                        .requestMatchers(
                                "/api/admin/**")
                        .hasRole("ADMIN")

                        // ── Everything else needs JWT ──
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter,
                        UsernamePasswordAuthenticationFilter
                                .class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager
    authenticationManager(
            AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }
}