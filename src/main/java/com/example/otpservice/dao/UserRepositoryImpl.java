package com.example.otpservice.dao;

import com.example.otpservice.model.User;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryImpl implements UserRepository {

    private final JdbcTemplate jdbc;

    public UserRepositoryImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private final RowMapper<User> userMapper = (rs, rowNum) -> {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(User.Role.valueOf(rs.getString("role")));
        return user;
    };

    @Override
    public Optional<User> findByUsername(String username) {
        return jdbc.query("SELECT * FROM users WHERE username = ? LIMIT 1", userMapper, username)
                .stream().findFirst();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jdbc.query("SELECT * FROM users WHERE email = ? LIMIT 1", userMapper, email)
                .stream().findFirst();
    }

    @Override
    public Optional<User> findByRole(User.Role role) {
        return jdbc.query("SELECT * FROM users WHERE role = ? LIMIT 1", userMapper, role.name())
                .stream().findFirst();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jdbc.query("SELECT * FROM users WHERE id = ? LIMIT 1", userMapper, id)
                .stream()
                .findFirst();
    }

    @Override
    public void save(User user) {
        jdbc.update(
                "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)",
                user.getUsername(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole().name()
        );
    }

    @Override
    public boolean adminExists() {
        return findByRole(User.Role.ADMIN).isPresent();
    }
}