package com.example.otpservice.dao;

import com.example.otpservice.model.OtpCode;
import com.example.otpservice.model.OtpStatus;

import java.util.List;
import java.util.Optional;


/**
 * DAO interface for OTP code operations.
 */
public interface OtpCodeRepository {
    List<OtpCode> findAllActive();
    void save(OtpCode otpCode);
    boolean existsByUserIdAndOperationId(Long userId, String operationId);

    Optional<OtpCode> findActiveCode(Long userId, String operationId, String code);
    void updateStatus(Long id, OtpStatus status);
}