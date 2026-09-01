package com.patechltd.Salexfy.controller;

import com.patechltd.Salexfy.dto.PushRequest;
import com.patechltd.Salexfy.dto.PushResponse;
import com.patechltd.Salexfy.dto.PullResponse;
import com.patechltd.Salexfy.service.AuthService;
import com.patechltd.Salexfy.service.SyncService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService sync;
    private final AuthService authService;

    public SyncController(SyncService sync, AuthService authService) {
        this.sync = sync;
        this.authService = authService;
    }

    @PostMapping("/push")
    public ResponseEntity<?> push(@RequestBody PushRequest request,
                                  HttpServletRequest httpRequest) {
        String shopId = resolveShopId(httpRequest);
        if (shopId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid or expired token"));
        }
        return ResponseEntity.ok(new PushResponse(sync.push(request, shopId)));
    }

    @GetMapping("/pull")
    public ResponseEntity<?> pull(@RequestParam(defaultValue = "0") long since,
                                  @RequestParam(required = false) String deviceId,
                                  HttpServletRequest httpRequest) {
        String shopId = resolveShopId(httpRequest);
        if (shopId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Invalid or expired token"));
        }
        return ResponseEntity.ok(new PullResponse(sync.pull(since, deviceId, shopId)));
    }

    private String resolveShopId(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        return authService.shopIdForToken(token).orElse(null);
    }
}