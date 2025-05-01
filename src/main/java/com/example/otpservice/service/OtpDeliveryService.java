package com.example.otpservice.service;

/**
 * Interface for delivering OTP codes to users via different channels.
 */
public interface OtpDeliveryService {

    /**
     * Sends an OTP code to the specified email address.
     *
     * @param email the email address to which the OTP code should be sent
     * @param code  the OTP code to be delivered
     */
    void sendOtp(String email, String code);
}