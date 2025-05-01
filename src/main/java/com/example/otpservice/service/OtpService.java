package com.example.otpservice.service;

import com.example.otpservice.dao.OtpCodeRepository;
import com.example.otpservice.exception.OtpCodeAlreadyExistsException;
import com.example.otpservice.model.DeliveryChannel;
import com.example.otpservice.model.OtpCode;
import com.example.otpservice.model.OtpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service for OTP code generation and management.
 */
@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpCodeRepository otpCodeRepository;
    private final OtpDeliveryFactory otpDeliveryFactory;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.code.length}")
    private int codeLength;

    @Value("${otp.code.ttl-seconds}")
    private int ttlSeconds;

    @Value("${otp.delivery.channel}")
    private String deliveryChannel;

    /**
     * Constructs the OTP service with the required repositories and delivery service.
     *
     * @param otpCodeRepository  repository for OTP code storage
     * @param otpDeliveryFactory factory responsible for delivering OTP codes
     * @param userService        service for accessing user information
     */


    public OtpService(OtpCodeRepository otpCodeRepository, OtpDeliveryFactory otpDeliveryFactory, UserService userService) {
        this.otpCodeRepository = otpCodeRepository;
        this.otpDeliveryFactory = otpDeliveryFactory;
        this.userService = userService;
    }

    /**
     * Generates an OTP code, saves it to the database, and sends it to the user.
     *
     * @param userId      the user ID
     * @param operationId the operation ID
     */
    public void generateOtp(Long userId, String operationId) {
        logger.info("Generating OTP code for userId={} and operationId={}", userId, operationId);
        if (otpCodeRepository.existsByUserIdAndOperationId(userId, operationId)) {
            logger.warn("OTP code already exists for userId={} and operationId={}", userId, operationId);
            throw new OtpCodeAlreadyExistsException("OTP code already exists for this operation and user.");
        }

        String code = generateRandomCode();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiry = now.plusSeconds(ttlSeconds);

        OtpCode otpCode = new OtpCode();
        otpCode.setUserId(userId);
        otpCode.setCode(code);
        otpCode.setOperationId(operationId);
        otpCode.setStatus(OtpStatus.ACTIVE); // Correct usage of Enum here
        otpCode.setCreatedAt(now);
        otpCode.setExpiresAt(expiry);

        otpCodeRepository.save(otpCode);
        logger.info("OTP code generated and saved successfully for userId={} and operationId={}", userId, operationId);

        // Select delivery channel dynamically
        DeliveryChannel channel = DeliveryChannel.valueOf(deliveryChannel.toUpperCase());
        OtpDeliveryService deliveryService = otpDeliveryFactory.getService(channel);

        // Lookup user's email by userId
        String email = userService.getEmailByUserId(userId);
        logger.info("Sending OTP code via {} to {}", channel, email);

        // Send OTP
        deliveryService.sendOtp(email, code);
        logger.info("OTP code sent successfully for userId={} and operationId={}", userId, operationId);
    }

    /**
     * Validates an OTP code for a specific operation.
     *
     * @param userId the user ID
     * @param operationId the operation ID
     * @param code the OTP code to validate
     * @return true if the OTP is valid, false otherwise
     */
    public boolean validateOtp(Long userId, String operationId, String code) {
        logger.info("Validating OTP code for userId={} and operationId={}", userId, operationId);
        var otpOpt = otpCodeRepository.findActiveCode(userId, operationId, code);

        if (otpOpt.isEmpty()) {
            logger.warn("No active OTP code found for userId={} and operationId={}", userId, operationId);
            return false;
        }

        OtpCode otp = otpOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(otp.getExpiresAt())) {
            otpCodeRepository.updateStatus(otp.getId(), OtpStatus.EXPIRED);
            logger.info("OTP code expired for userId={} and operationId={}", userId, operationId);
            return false;
        }

        otpCodeRepository.updateStatus(otp.getId(), OtpStatus.USED);
        logger.info("OTP code successfully validated and marked as USED for userId={} and operationId={}", userId, operationId);
        return true;
    }

    private String generateRandomCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            sb.append(secureRandom.nextInt(10));
        }
        return sb.toString();
    }
}
