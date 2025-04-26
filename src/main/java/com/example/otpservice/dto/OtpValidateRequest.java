package com.example.otpservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for OTP code validation request.
 */
public class OtpValidateRequest {

    @NotBlank(message = "Operation ID must not be blank")
    private String operationId;

    @NotBlank(message = "OTP code must not be blank")
    private String code;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}