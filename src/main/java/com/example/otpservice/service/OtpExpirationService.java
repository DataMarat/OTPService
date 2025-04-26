package com.example.otpservice.service;

import com.example.otpservice.dao.OtpCodeRepository;
import com.example.otpservice.model.OtpCode;
import com.example.otpservice.model.OtpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for expiring OTP codes automatically.
 */
@Service
public class OtpExpirationService {

    private final OtpCodeRepository otpCodeRepository;

    public OtpExpirationService(OtpCodeRepository otpCodeRepository) {
        this.otpCodeRepository = otpCodeRepository;
    }

    /**
     * Scheduled task to expire OTP codes that have passed their expiration time.
     * Runs every 5 minutes.
     */
    @Scheduled(fixedRate = 300000) // every 5 minutes
    public void expireOtpCodes() {
        List<OtpCode> activeCodes = otpCodeRepository.findAllActive();
        LocalDateTime now = LocalDateTime.now();

        for (OtpCode otpCode : activeCodes) {
            if (now.isAfter(otpCode.getExpiresAt())) {
                otpCodeRepository.updateStatus(otpCode.getId(), OtpStatus.EXPIRED);
            }
        }
    }
}