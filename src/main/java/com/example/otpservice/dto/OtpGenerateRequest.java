package com.example.otpservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for OTP generation request.
 */
public class OtpGenerateRequest {

    @NotBlank(message = "Operation ID must not be blank")
    private String operationId;

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }
}