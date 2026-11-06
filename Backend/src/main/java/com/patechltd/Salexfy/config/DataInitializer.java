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

    @Value("${salexfy.admin.password:}")
    private String adminPassword;

    public DataInitializer(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public void run(String... args) {
        String pw = adminPassword;
        boolean generated = pw == null || pw.isBlank();
        if (generated) {
            byte[] b = new byte[18];
            new java.security.SecureRandom().nextBytes(b);
            pw = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        }
        boolean created = auth.createAdminIfMissing(adminUsername, pw);
        if (created && generated) {
            org.slf4j.LoggerFactory.getLogger(DataInitializer.class)
                    .warn("Initial admin created. username={} password={} (shown once - change it)", adminUsername, pw);
        }
    }
}
