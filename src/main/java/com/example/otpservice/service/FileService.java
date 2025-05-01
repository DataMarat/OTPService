package com.example.otpservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Service for saving OTP codes to a local text file in the project root directory.
 */
@Service
public class FileService implements OtpDeliveryService {

    @Value("${otp.file.name}")
    private String fileName;

    /**
     * Saves an OTP code to a local text file.
     *
     * @param email the email address of the user
     * @param code  the OTP code to be saved
     */
    @Override
    public void sendOtp(String email, String code) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true))) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String record = String.format("%s - User ID: %d, OTP Code: %s", timestamp, email, code);
            writer.write(record);
            writer.newLine();
        } catch (IOException e) {
            throw new RuntimeException("Failed to save OTP code to file", e);
        }
    }
}