package com.example.otpservice.controller;

import com.example.otpservice.dto.OtpGenerateRequest;
import com.example.otpservice.dto.OtpValidateRequest;
import com.example.otpservice.service.OtpService;
import com.example.otpservice.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling OTP generation requests.
 */
@RestController
@RequestMapping("/otp")
public class OtpController {
    private static final Logger logger = LoggerFactory.getLogger(OtpController.class);
    private final OtpService otpService;
    private final UserService userService;

    public OtpController(OtpService otpService, UserService userService) {
        this.otpService = otpService;
        this.userService = userService;
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
        String email = authentication.getName();
        Long userId = userService.getUserIdByEmail(email);
        logger.info("Received OTP generation request for userId={} and operationId={}", userId, request.getOperationId());

        otpService.generateOtp(userId, request.getOperationId());
        logger.info("OTP code successfully processed for userId={} and operationId={}", userId, request.getOperationId());

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

        String email = authentication.getName();
        Long userId = userService.getUserIdByEmail(email);
        logger.info("Received OTP validation request for userId={} and operationId={}", userId, request.getOperationId());
        boolean isValid = otpService.validateOtp(userId, request.getOperationId(), request.getCode());
        if (isValid) {
            logger.info("OTP code validated successfully for userId={} and operationId={}", userId, request.getOperationId());
            return ResponseEntity.ok("OTP code is valid");
        } else {
            logger.warn("Failed to validate OTP code for userId={} and operationId={}", userId, request.getOperationId());
            return ResponseEntity.badRequest().body("Invalid or expired OTP code");
        }
    }
}