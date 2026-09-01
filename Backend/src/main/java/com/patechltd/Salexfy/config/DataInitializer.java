package com.patechltd.Salexfy.config;

import com.patechltd.Salexfy.service.AuthService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AuthService auth;

    @Value("${salexfy.admin.username:admin}")
    private String adminUsername;

    @Value("${salexfy.admin.password:admin123}")
    private String adminPassword;

    public DataInitializer(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void run(String... args) {
        auth.ensureAdmin(adminUsername, adminPassword);
    }
}
