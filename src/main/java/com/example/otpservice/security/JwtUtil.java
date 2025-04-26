package com.example.otpservice.security;

import com.example.otpservice.model.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Utility class for generating JWT tokens based on user information.
 */
@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    /**
     * Generates a JWT token for the given user.
     *
     * @param user the user entity
     * @return a signed JWT token string
     */
    public String generateToken(User user) {
        logger.debug("Generating JWT token for user '{}'", user.getUsername());

        String token = Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();

        logger.info("Token generated successfully for user '{}'", user.getUsername());
        return token;
    }
}