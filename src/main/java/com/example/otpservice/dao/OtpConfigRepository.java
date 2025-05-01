package com.example.otpservice.dao;

import com.example.otpservice.model.OtpConfig;

public interface OtpConfigRepository {
    OtpConfig getConfig();
    void updateConfig(OtpConfig config);
}