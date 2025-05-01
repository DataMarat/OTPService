package com.example.otpservice.exception;

/**
 * Exception thrown when an active OTP code already exists for a given user and operation.
 */
public class OtpCodeAlreadyExistsException extends RuntimeException {

    public OtpCodeAlreadyExistsException(String message) {
        super(message);
    }
}