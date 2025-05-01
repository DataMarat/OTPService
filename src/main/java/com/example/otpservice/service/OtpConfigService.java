package com.example.otpservice.service;

import com.example.otpservice.dao.OtpConfigRepository;
import com.example.otpservice.model.OtpConfig;
import org.springframework.stereotype.Service;

@Service
public class OtpConfigService {
    private final OtpConfigRepository configRepository;

    public OtpConfigService(OtpConfigRepository configRepository) {
        this.configRepository = configRepository;
    }

    public OtpConfig getConfig() {
        return configRepository.getConfig();
    }

    public void updateConfig(int length, int ttl) {
        configRepository.updateConfig(new OtpConfig(length, ttl));
    }
}