package com.example.otpservice.service;

import com.example.otpservice.dao.UserRepository;
import com.example.otpservice.model.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(); // можно позже внедрить как бин
    }

    public void registerUser(String username, String email, String rawPassword, boolean isAdmin) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        if (isAdmin && userRepository.adminExists()) {
            throw new IllegalStateException("Admin already exists");
        }

        String passwordHash = passwordEncoder.encode(rawPassword);

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(isAdmin ? User.Role.ADMIN : User.Role.USER);

        userRepository.save(user);
    }
}