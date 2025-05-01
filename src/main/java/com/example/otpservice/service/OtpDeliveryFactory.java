package com.example.otpservice.service;

import com.example.otpservice.model.DeliveryChannel;
import org.springframework.stereotype.Component;

/**
 * Factory class to select the correct OtpDeliveryService based on the delivery channel.
 */
@Component
public class OtpDeliveryFactory {

    private final EmailService emailService;
    private final TelegramService telegramService;
    private final SmsService smsService;
    private final FileService fileService;

    public OtpDeliveryFactory(EmailService emailService,
                              TelegramService telegramService,
                              SmsService smsService,
                              FileService fileService) {
        this.emailService = emailService;
        this.telegramService = telegramService;
        this.smsService = smsService;
        this.fileService = fileService;
    }

    /**
     * Returns the appropriate OtpDeliveryService based on the delivery channel.
     *
     * @param channel the delivery channel
     * @return the corresponding OtpDeliveryService
     */
    public OtpDeliveryService getService(DeliveryChannel channel) {
        return switch (channel) {
            case EMAIL -> emailService;
            case TELEGRAM -> telegramService;
            case SMS -> smsService;
            case FILE -> fileService;
        };
    }
}