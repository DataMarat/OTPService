package com.example.otpservice.service;

import com.example.otpservice.dao.OtpCodeRepository;
import com.example.otpservice.model.OtpCode;
import com.example.otpservice.model.OtpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service for OTP code generation and management.
 */
@Service
public class OtpService {
    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);
    private final OtpCodeRepository otpCodeRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${otp.code.length}")
    private int codeLength;

    @Value("${otp.code.ttl-seconds}")
    private int ttlSeconds;

    public OtpService(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    /**
     * Generates an OTP code and saves it to the database.
     *
     * @param userId      the user ID
     * @param operationId the operation ID
     */
    public void generateOtp(Long userId, String operationId) {
        logger.info("Generating OTP code for userId={} and operationId={}", userId, operationId);
        if (otpCodeRepository.existsByUserIdAndOperationId(userId, operationId)) {
            logger.warn("OTP code already exists for userId={} and operationId={}", userId, operationId);
            throw new IllegalStateException("OTP code already exists for this operation and user.");
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
