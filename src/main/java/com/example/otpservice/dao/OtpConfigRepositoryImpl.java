package com.example.otpservice.dao;

import com.example.otpservice.model.OtpConfig;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class OtpConfigRepositoryImpl implements OtpConfigRepository {

    private final JdbcTemplate jdbc;

    public OtpConfigRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OtpConfig getConfig() {
        return jdbc.queryForObject(
                "SELECT code_length, ttl_seconds FROM otp_config LIMIT 1",
                (rs, rowNum) -> new OtpConfig(
                        rs.getInt("code_length"),
                        rs.getInt("ttl_seconds")
                )
        );
    }

    @Override
    public void updateConfig(OtpConfig config) {
        jdbc.update("UPDATE otp_config SET code_length = ?, ttl_seconds = ?",
                config.getCodeLength(), config.getTtlSeconds());
    }
}