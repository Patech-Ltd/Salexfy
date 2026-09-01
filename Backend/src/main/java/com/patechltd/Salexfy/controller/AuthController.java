package com.patechltd.Salexfy.controller;

import com.patechltd.Salexfy.dto.LoginRequest;
import com.patechltd.Salexfy.dto.LoginResponse;
import com.patechltd.Salexfy.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        return auth.login(request.username(), request.password())
                .<ResponseEntity<?>>map(token -> {
                    String shopId = auth.shopIdForToken(token).orElse("default");
                    return ResponseEntity.ok(new LoginResponse(token, shopId));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Invalid username or password")));
    }
}