package com.example.otpservice.service;

import com.example.otpservice.dao.OtpCodeRepository;
import com.example.otpservice.model.OtpCode;
import com.example.otpservice.model.OtpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Service for OTP code generation and management.
 */
@Service
public class OtpService {

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
        if (otpCodeRepository.existsByUserIdAndOperationId(userId, operationId)) {
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
        var otpOpt = otpCodeRepository.findActiveCode(userId, operationId, code);

        if (otpOpt.isEmpty()) {
            return false;
        }

        OtpCode otp = otpOpt.get();
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(otp.getExpiresAt())) {
            otpCodeRepository.updateStatus(otp.getId(), OtpStatus.EXPIRED);
            return false;
        }

        otpCodeRepository.updateStatus(otp.getId(), OtpStatus.USED);
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
