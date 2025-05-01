package com.example.otpservice.service;

import org.smpp.Connection;
import org.smpp.Session;
import org.smpp.TCPIPConnection;
import org.smpp.pdu.BindTransmitter;
import org.smpp.pdu.BindResponse;
import org.smpp.pdu.PDUException;
import org.smpp.pdu.SubmitSM;
import org.smpp.pdu.ValueNotSetException;
import org.smpp.WrongSessionStateException;
import org.smpp.TimeoutException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;

/**
 * Service for sending OTP codes to users via SMPP emulator.
 */
@Service
public class SmsService implements OtpDeliveryService {

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

    /**
     * Sends an OTP code to a user via SMPP emulator.
     *
     * @param email the email address of the user (ignored for SMS)
     * @param code  the OTP code to be sent
     */
    @Override
    public void sendOtp(String email, String code) {
        Connection connection = null;
        Session session = null;

        try {
            // Establish TCP/IP connection
            connection = new TCPIPConnection(host, port);
            session = new Session(connection);

            // Bind to the SMPP server
            BindTransmitter bindRequest = new BindTransmitter();
            bindRequest.setSystemId(systemId);
            bindRequest.setPassword(password);
            bindRequest.setSystemType(systemType);
            bindRequest.setInterfaceVersion((byte) 0x34); // SMPP v3.4
            bindRequest.setAddressRange(sourceAddress);

            BindResponse bindResponse = session.bind(bindRequest);
            if (bindResponse.getCommandStatus() != 0) {
                throw new RuntimeException("SMPP Bind failed with status: " + bindResponse.getCommandStatus());
            }

            // Prepare the SMS message
            SubmitSM submitSM = new SubmitSM();
            submitSM.setSourceAddr(sourceAddress);

            // In real system: lookup destination phone number by userId
            submitSM.setDestAddr("1234567890");
            submitSM.setShortMessage(Arrays.toString(("Your OTP code is: " + code).getBytes()));

            // Send the SMS message
            session.submit(submitSM);

        } catch (ValueNotSetException e) {
            throw new RuntimeException("Required SMPP field is not set", e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Timeout during SMPP communication", e);
        } catch (PDUException e) {
            throw new RuntimeException("PDU structure error in SMPP communication", e);
        } catch (WrongSessionStateException e) {
            throw new RuntimeException("Wrong session state for SMPP operation", e);
        } catch (IOException e) {
            throw new RuntimeException("I/O error during SMPP communication", e);
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error during SMPP communication", e);
        } finally {
            if (session != null) {
                try {
                    session.unbind();
                    session.close();
                } catch (Exception ignored) {
                    // Ignore errors during session close
                }
            }
        }
    }
}
