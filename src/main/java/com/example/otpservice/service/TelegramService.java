package com.example.otpservice.service;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Service for sending OTP codes to users via Telegram Bot API.
 */
@Service
public class TelegramService implements OtpDeliveryService {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.chat.id}")
    private String chatId;

    private static final String TELEGRAM_API_URL = "https://api.telegram.org/bot";

    /**
     * Sends an OTP code to the Telegram chat.
     *
     * @param email the email address of the user (ignored for Telegram)
     * @param code  the OTP code to be sent
     */
    @Override
    public void sendOtp(String email, String code) {
        String message = String.format("Your OTP code is: %s", code);
        String encodedMessage = urlEncode(message);
        String url = String.format("%s%s/sendMessage?chat_id=%s&text=%s",
                TELEGRAM_API_URL,
                botToken,
                chatId,
                encodedMessage
        );

        sendTelegramRequest(url);
    }

    /**
     * Sends an HTTP GET request to the Telegram API to deliver the message.
     *
     * @param url the fully constructed Telegram API URL
     */
    private void sendTelegramRequest(String url) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode != 200) {
                    throw new RuntimeException("Failed to send OTP via Telegram. Status code: " + statusCode);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error sending request to Telegram API", e);
        }
    }

    /**
     * Encodes a URL parameter using UTF-8 encoding.
     *
     * @param value the value to be encoded
     * @return the encoded value
     */
    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}