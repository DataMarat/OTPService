package com.example.otpservice.service;

import com.example.otpservice.dao.UserRepository;
import com.example.otpservice.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Service responsible for user registration and validation logic.
 */
@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(); // можно позже внедрить как бин
    }

    /**
     * Registers a new user, ensuring uniqueness of username/email and admin constraints.
     *
     * @param username    the desired username
     * @param email       the email address
     * @param rawPassword plain text password
     * @param isAdmin     true if the user should be an admin
     */
    public void registerUser(String username, String email, String rawPassword, boolean isAdmin) {
        if (userRepository.findByUsername(username).isPresent()) {
            logger.warn("Registration failed: username '{}' already exists", username);
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            logger.warn("Registration failed: email '{}' already exists", email);
            throw new IllegalArgumentException("Email already exists");
        }

        if (isAdmin && userRepository.adminExists()) {
            logger.warn("Registration failed: attempted to create a second admin");
            throw new IllegalStateException("Admin already exists");
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        logger.debug("Password successfully hashed for username '{}'", username);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(isAdmin ? User.Role.ADMIN : User.Role.USER);

        userRepository.save(user);
        logger.info("User '{}' registered successfully with role '{}'", username, user.getRole());
    }

    /**
     * Authenticates a user using email and password.
     *
     * @param email user email
     * @param rawPassword raw password input
     * @return User object if authentication is successful
     * @throws IllegalArgumentException if email is not found or password is invalid
     */
    public User authenticateUser(String email, String rawPassword) {
        logger.info("Authenticating user with email '{}'", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("Authentication failed: email '{}' not found", email);
                    return new IllegalArgumentException("Invalid email or password");
                });

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            logger.warn("Authentication failed: incorrect password for email '{}'", email);
            throw new IllegalArgumentException("Invalid email or password");
        }

        logger.info("User '{}' authenticated successfully", user.getUsername());
        return user;
    }

    /**
     * Finds a user ID by email.
     *
     * @param email the user's email
     * @return the user ID
     */
    public Long getUserIdByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email))
                .getId();
    }

    /**
     * Finds a user's email by their ID.
     *
     * @param userId the user's ID
     * @return the user's email
     */
    public String getEmailByUserId(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId))
                .getEmail();
    }
}