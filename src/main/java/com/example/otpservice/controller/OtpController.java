package com.example.otpservice.controller;

import com.example.otpservice.dto.OtpGenerateRequest;
import com.example.otpservice.dto.OtpValidateRequest;
import com.example.otpservice.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling OTP generation requests.
 */
@RestController
@RequestMapping("/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    /**
     * Generates a new OTP code for the specified operation.
     *
     * @param request         the OTP generation request
     * @param authentication  the authenticated user information
     * @return HTTP 201 Created response
     */
    @PostMapping("/generate")
    public ResponseEntity<Void> generateOtp(@RequestBody @Valid OtpGenerateRequest request,
                                            Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName()); // Assuming user ID is stored as principal
        otpService.generateOtp(userId, request.getOperationId());
        return ResponseEntity.status(201).build();
    }

    /**
     * Validates an OTP code for a specific operation.
     *
     * @param request         the OTP validation request
     * @param authentication  the authenticated user information
     * @return 200 OK if valid, 400 Bad Request if invalid
     */
    @PostMapping("/validate")
    public ResponseEntity<String> validateOtp(@RequestBody @Valid OtpValidateRequest request,
                                              Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        boolean isValid = otpService.validateOtp(userId, request.getOperationId(), request.getCode());
        if (isValid) {
            return ResponseEntity.ok("OTP code is valid");
        } else {
            return ResponseEntity.badRequest().body("Invalid or expired OTP code");
        }
    }
}