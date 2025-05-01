package com.example.otpservice.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smpp.Connection;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.TimeoutException;
import org.smpp.WrongSessionStateException;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.ValueNotSetException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;

/**
 * Service for sending OTP codes to users via SMPP emulator.
 */
@Service
public class SmsService implements OtpDeliveryService {
    private static final Logger logger = LoggerFactory.getLogger(SmsService.class);

    @Value("${smpp.host}")
    private String host;

    @Value("${smpp.port}")
    private int port;

    @Value("${smpp.system_id}")
    private String systemId;

    @Value("${smpp.password}")
    private String password;

    @Value("${smpp.system_type}")
    private String systemType;

    @Value("${smpp.source_addr}")
    private String sourceAddress;

    private final UserService userService;

    public SmsService(UserService userService) {
        this.userService = userService;
    }
    /**
     * Sends an OTP code to the user's phone number using their email as identifier.
     *
     * @param email the email address of the user
     * @param code  the OTP code to be sent
     */
    @Override
    public void sendOtp(String email, String code) {
        String phoneNumber = userService.getPhoneByEmail(email);
        String message = "Ваш OTP-код: " + code + ". Не сообщайте его никому.";

        logger.info("Preparing to send OTP via SMS to phone {} for user '{}'", phoneNumber, email);
        sendSms(phoneNumber, message);
    }
    /**
     * Sends an SMS message to the specified phone number via SMPP.
     *
     * @param phoneNumber recipient's phone number
     * @param message     message content
     */
    public void sendSms(String phoneNumber, String message) {
        Connection connection = null;
        Session session = null;

        try {
            connection = new TCPIPConnection(host, port);
            session = new Session(connection);

            BindTransmitter bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);
            bindRequest.setSystemType(systemType);
            bindRequest.setInterfaceVersion((byte) 0x34);
            bindRequest.setAddressRange(sourceAddress);

            logger.debug("Connecting to SMPP at {}:{}", host, port);
            BindResponse bindResponse = session.bind(bindRequest);

            if (bindResponse.getCommandStatus() != 0) {
                throw new RuntimeException("SMPP Bind failed with status: " + bindResponse.getCommandStatus());
            }

            SubmitSM submitSM = new SubmitSM();
            submitSM.setSourceAddr(sourceAddress);
            submitSM.setDestAddr(phoneNumber);
            submitSM.setShortMessage(message);

            session.submit(submitSM);

        } catch (PDUException | TimeoutException | WrongSessionStateException | IOException e) {
            logger.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            throw new RuntimeException("SMPP sending failed: " + e.getMessage(), e);
        } finally {
            if (session != null) {
                try {
                    session.unbind();
                    session.close();
                    logger.debug("SMPP session closed");
                } catch (Exception e) {
                    logger.warn("Error closing SMPP session: {}", e.getMessage());
                }
            }
        }
    }
}
