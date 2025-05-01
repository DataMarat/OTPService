package com.example.otpservice.controller;

import com.example.otpservice.dto.OtpConfigRequest;
import com.example.otpservice.model.OtpConfig;
import com.example.otpservice.model.User;
import com.example.otpservice.dao.OtpCodeRepository;
import com.example.otpservice.service.OtpConfigService;
import com.example.otpservice.dao.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@PreAuthorize("hasAuthority('ADMIN')")
public class AdminController {
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private final OtpConfigService configService;
    private final UserRepository userRepository;
    private final OtpCodeRepository otpCodeRepository;

    public AdminController(OtpConfigService configService, UserRepository userRepository, OtpCodeRepository otpCodeRepository) {
        this.configService = configService;
        this.userRepository = userRepository;
        this.otpCodeRepository = otpCodeRepository;
    }

    @GetMapping("/otp-config")
    public OtpConfig getConfig() {
        logger.info("[ADMIN] Requested current OTP configuration");
        return configService.getConfig();
    }

    @PutMapping("/otp-config")
    public ResponseEntity<Void> updateConfig(@RequestBody @Valid OtpConfigRequest request) {
        logger.info("[ADMIN] Updating OTP configuration: codeLength={}, ttlSeconds={}",
                request.getCodeLength(), request.getTtlSeconds());
        configService.updateConfig(request.getCodeLength(), request.getTtlSeconds());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/users")
    public List<User> listNonAdmins() {
        logger.info("[ADMIN] Requested list of all non-admin users");
        return userRepository.findAllNonAdmins();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.warn("[ADMIN] Deleting user with id={} and related OTP codes", id);
        otpCodeRepository.deleteByUserId(id);
        userRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}