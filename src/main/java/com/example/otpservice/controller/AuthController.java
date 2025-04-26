package com.example.otpservice.controller;

import com.example.otpservice.dto.LoginRequest;
import com.example.otpservice.dto.JwtResponse;
import com.example.otpservice.model.User;
import com.example.otpservice.security.JwtUtil;
import com.example.otpservice.dto.RegistrationRequest;
import com.example.otpservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.Authentication;

/**
 * Controller for handling user registration.
 */
@RestController
@RequestMapping
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    /**
     * POST /register
     * Handles user registration with role constraints and unique checks.
     *
     * @param request RegistrationRequest with user input
     * @return ResponseEntity with status and body
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody @Valid RegistrationRequest request,
            HttpServletRequest httpRequest
    ) {
        logger.info("[POST /register] Registration attempt: username='{}', email='{}', admin={}",
                request.getUsername(), request.getEmail(), request.getAdmin());

        try {
            userService.registerUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getAdmin()
            );
            logger.info("User '{}' registered successfully", request.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (IllegalArgumentException | IllegalStateException e) {
            HttpStatus status = (e instanceof IllegalArgumentException)
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.CONFLICT;

            logger.warn("Registration failed for '{}': {} ({})",
                    request.getUsername(), e.getMessage(), status);

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", ZonedDateTime.now());
            body.put("status", status.value());
            body.put("error", status.getReasonPhrase());
            body.put("message", e.getMessage());
            body.put("path", httpRequest.getRequestURI());

            return new ResponseEntity<>(body, status);
        }
    }

    /**
     * POST /login
     * Handles user authentication and JWT generation.
     *
     * @param request LoginRequest with email and password
     * @return JwtResponse with JWT token or 401 Unauthorized
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletRequest httpRequest) {
        logger.info("[POST /login] Login attempt: email='{}'", request.getEmail());

        try {
            User user = userService.authenticateUser(request.getEmail(), request.getPassword());
            String token = jwtUtil.generateToken(user);
            logger.info("Login successful for email='{}'", request.getEmail());
            return ResponseEntity.ok(new JwtResponse(token));

        } catch (IllegalArgumentException e) {
            logger.warn("Login failed for email='{}': {}", request.getEmail(), e.getMessage());

            Map<String, Object> body = new HashMap<>();
            body.put("timestamp", ZonedDateTime.now());
            body.put("status", HttpStatus.UNAUTHORIZED.value());
            body.put("error", "Unauthorized");
            body.put("message", e.getMessage());
            body.put("path", httpRequest.getRequestURI());

            return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * GET /me
     * Returns basic information about the authenticated user.
     *
     * @param auth Authentication object injected by Spring Security
     * @return user's email and role
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me(Authentication auth) {
        Map<String, Object> response = new HashMap<>();
        response.put("email", auth.getName());
        response.put("roles", auth.getAuthorities());

        logger.info("[GET /me] Requested by '{}'", auth.getName());

        return ResponseEntity.ok(response);
    }

}