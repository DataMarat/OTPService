package com.example.otpservice.dao;

import com.example.otpservice.model.OtpCode;
import com.example.otpservice.model.OtpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of the OtpCodeRepository interface using JDBC for data access.
 * This class provides methods to save and retrieve OTP code records from the database.
 */
@Repository
public class OtpCodeRepositoryImpl implements OtpCodeRepository {
    private final JdbcTemplate jdbcTemplate;

    public OtpCodeRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(OtpCode otpCode) {
        String sql = "INSERT INTO otp_codes (user_id, code, operation_id, status, created_at, expires_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                otpCode.getUserId(),
                otpCode.getCode(),
                otpCode.getOperationId(),
                otpCode.getStatus().name(),
                otpCode.getCreatedAt(),
                otpCode.getExpiresAt()
        );
    }

    @Override
    public boolean existsByUserIdAndOperationId(Long userId, String operationId) {
        String sql = "SELECT COUNT(*) FROM otp_codes WHERE user_id = ? AND operation_id = ? AND status = 'ACTIVE'";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, userId, operationId);
        return count != null && count > 0;
    }

    @Override
    public Optional<OtpCode> findActiveCode(Long userId, String operationId, String code) {
        String sql = "SELECT * FROM otp_codes WHERE user_id = ? AND operation_id = ? AND code = ? AND status = 'ACTIVE'";
        var results = jdbcTemplate.query(sql, (rs, rowNum) -> {
            OtpCode otpCode = new OtpCode();
            otpCode.setId(rs.getLong("id"));
            otpCode.setUserId(rs.getLong("user_id"));
            otpCode.setCode(rs.getString("code"));
            otpCode.setOperationId(rs.getString("operation_id"));
            otpCode.setStatus(OtpStatus.valueOf(rs.getString("status")));
            otpCode.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            otpCode.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
            return otpCode;
        }, userId, operationId, code);

        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(results.get(0));
    }

    @Override
    public void updateStatus(Long id, OtpStatus status) {
        String sql = "UPDATE otp_codes SET status = ? WHERE id = ?";
        jdbcTemplate.update(sql, status.name(), id);
    }

    @Override
    public List<OtpCode> findAllActive() {
        String sql = "SELECT * FROM otp_codes WHERE status = 'ACTIVE'";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            OtpCode otpCode = new OtpCode();
            otpCode.setId(rs.getLong("id"));
            otpCode.setUserId(rs.getLong("user_id"));
            otpCode.setCode(rs.getString("code"));
            otpCode.setOperationId(rs.getString("operation_id"));
            otpCode.setStatus(OtpStatus.valueOf(rs.getString("status")));
            otpCode.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
            otpCode.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
            return otpCode;
        });
    }

    @Override
    public void deleteByUserId(Long userId) {
        jdbcTemplate.update("DELETE FROM otp_codes WHERE user_id = ?", userId);
    }
}