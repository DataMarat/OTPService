package com.example.otpservice.controller;

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

/**
 * Controller for handling user registration.
 */
@RestController
@RequestMapping
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
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
}