package com.example.musicdigging.controller;

import com.example.musicdigging.digging.model.DiggingResult;
import com.example.musicdigging.digging.service.DiggingService;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/digging")
@Profile("local")
public class DiggingController {

    private final DiggingService diggingService;

    public DiggingController(DiggingService diggingService) {
        this.diggingService = diggingService;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of(
                "status", "ok",
                "message", "디깅 API 연결 성공"
        );
    }

    @GetMapping
    public DiggingResult dig(
            @RequestParam("recordingId") String recordingId
    ) {
        return diggingService.dig(recordingId);
    }
}