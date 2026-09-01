package com.patechltd.Salexfy.controller;

import com.patechltd.Salexfy.dto.PullResponse;
import com.patechltd.Salexfy.dto.PushRequest;
import com.patechltd.Salexfy.dto.PushResponse;
import com.patechltd.Salexfy.service.SyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class SyncController {

    private final SyncService sync;

    public SyncController(SyncService sync) {
        this.sync = sync;
    }

    @PostMapping("/push")
    public ResponseEntity<PushResponse> push(@RequestBody PushRequest request) {
        return ResponseEntity.ok(new PushResponse(sync.push(request)));
    }

    @GetMapping("/pull")
    public ResponseEntity<PullResponse> pull(@RequestParam(defaultValue = "0") long since,
                                             @RequestParam(required = false) String deviceId) {
        return ResponseEntity.ok(new PullResponse(sync.pull(since, deviceId)));
    }
}
