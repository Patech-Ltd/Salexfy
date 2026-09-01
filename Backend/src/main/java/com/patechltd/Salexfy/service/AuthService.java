package com.patechltd.Salexfy.service;

import com.patechltd.Salexfy.entity.AppUser;
import com.patechltd.Salexfy.entity.AuthToken;
import com.patechltd.Salexfy.repo.AppUserRepository;
import com.patechltd.Salexfy.repo.AuthTokenRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private static final long TOKEN_TTL_MS = 30L * 24 * 60 * 60 * 1000;

    private final AppUserRepository users;
    private final AuthTokenRepository tokens;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();

    public AuthService(AppUserRepository users, AuthTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    public Optional<String> login(String username, String password) {
        if (username == null || password == null) return Optional.empty();
        return users.findByUsername(username.trim())
                .filter(AppUser::isActive)
                .filter(user -> encoder.matches(password, user.getPasswordHash()))
                .map(user -> issueToken(user.getUsername()));
    }

    public boolean isValidToken(String token) {
        if (token == null || token.isBlank()) return false;
        long now = System.currentTimeMillis();
        return tokens.findByToken(token.trim())
                .map(t -> {
                    if (t.getExpiresAt() < now) {
                        tokens.delete(t);
                        return false;
                    }
                    return true;
                })
                .orElse(false);
    }

    public AppUser ensureAdmin(String username, String password) {
        return users.findByUsername(username).orElseGet(() -> {
            AppUser admin = new AppUser();
            admin.setUid(UUID.randomUUID().toString());
            admin.setUsername(username);
            admin.setPasswordHash(encoder.encode(password));
            admin.setFullName("Administrator");
            admin.setRole("ADMIN");
            admin.setActive(true);
            return users.save(admin);
        });
    }

    private String issueToken(String username) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        AuthToken authToken = new AuthToken();
        authToken.setToken(token);
        authToken.setUsername(username);
        long now = System.currentTimeMillis();
        authToken.setCreatedAt(now);
        authToken.setExpiresAt(now + TOKEN_TTL_MS);
        tokens.save(authToken);
        return token;
    }
}
