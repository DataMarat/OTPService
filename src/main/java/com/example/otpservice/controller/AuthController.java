package com.example.otpservice.controller;

import com.example.otpservice.dto.RegistrationRequest;
import com.example.otpservice.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
        try {
            userService.registerUser(
                    request.getUsername(),
                    request.getEmail(),
                    request.getPassword(),
                    request.getAdmin()
            );
            return ResponseEntity.status(HttpStatus.CREATED).build();

        } catch (IllegalArgumentException | IllegalStateException e) {
            HttpStatus status = (e instanceof IllegalArgumentException)
                    ? HttpStatus.BAD_REQUEST
                    : HttpStatus.CONFLICT;

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