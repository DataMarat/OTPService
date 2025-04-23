package com.example.otpservice.dao;

import com.example.otpservice.model.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByRole(User.Role role);
    void save(User user);
    boolean adminExists();
}