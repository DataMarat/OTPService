package com.example.otpservice.service;

import com.example.otpservice.dao.OtpCodeRepository;
import com.example.otpservice.model.OtpCode;
import com.example.otpservice.model.OtpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service for expiring OTP codes automatically.
 */
@Service
public class OtpExpirationService {
    private static final Logger logger = LoggerFactory.getLogger(OtpExpirationService.class);
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
        logger.info("Starting scheduled OTP expiration task");

        List<OtpCode> activeCodes = otpCodeRepository.findAllActive();
        LocalDateTime now = LocalDateTime.now();
        int expiredCount = 0;

        for (OtpCode otpCode : activeCodes) {
            if (now.isAfter(otpCode.getExpiresAt())) {
                otpCodeRepository.updateStatus(otpCode.getId(), OtpStatus.EXPIRED);
                logger.info("OTP code with id={} expired and status updated", otpCode.getId());
                expiredCount++;
            }
        }

        logger.info("Scheduled OTP expiration task completed. {} codes expired.", expiredCount);
    }
}