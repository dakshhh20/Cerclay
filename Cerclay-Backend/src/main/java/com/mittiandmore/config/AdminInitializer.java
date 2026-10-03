package com.mittiandmore.config;

import com.mittiandmore.entity.Admin;
import com.mittiandmore.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminInitializer implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    public AdminInitializer(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder) {

        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (adminRepository.findByEmail(adminEmail).isPresent()) {
            return;
        }

        Admin admin = new Admin();

        admin.setName("Store Admin");
        admin.setEmail(adminEmail);

        admin.setPassword(
                passwordEncoder.encode(adminPassword)
        );

        admin.setActive(true);
        admin.setRole("ADMIN");

        adminRepository.save(admin);

        System.out.println("Initial admin account created.");
    }
}