package com.example.otp_service;

import com.example.otp_service.dao.UserRepository;
import com.example.otp_service.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication(scanBasePackages = {"com.example.otp_service", "com.example.otpservice"})
public class OtpServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(OtpServiceApplication.class, args);
	}

	@Bean
	public CommandLineRunner demo(UserRepository repo) {
		return args -> {
			System.out.println("Админ существует? " + repo.adminExists());
		};
	}

	@Bean
	public CommandLineRunner testRegistration(UserService userService) {
		return args -> {
			try {
				userService.registerUser("admin", "admin@example.com", "password123", true);
				System.out.println("✅ Пользователь admin зарегистрирован");
			} catch (Exception e) {
				System.out.println("❌ Ошибка регистрации: " + e.getMessage());
			}
		};
	}
}
