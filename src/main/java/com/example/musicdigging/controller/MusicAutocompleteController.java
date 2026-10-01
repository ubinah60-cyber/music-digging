package com.example.musicdigging.controller;

import com.example.musicdigging.digging.provider.MusicDataLookupException;
import com.example.musicdigging.dto.MusicAutocompleteResponse;
import com.example.musicdigging.service.MusicAutocompleteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MusicAutocompleteController {

    private final MusicAutocompleteService service;

    public MusicAutocompleteController(MusicAutocompleteService service) {
        this.service = service;
    }

    @GetMapping("/api/music/autocomplete")
    public MusicAutocompleteResponse autocomplete(@RequestParam String query) {
        return service.search(query);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> invalidQuery(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of(
                "code", "INVALID_QUERY", "message", exception.getMessage()));
    }

    @ExceptionHandler(MusicDataLookupException.class)
    public ResponseEntity<Map<String, String>> unavailable(MusicDataLookupException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of(
                "code", "MUSIC_DATA_LOOKUP_FAILED", "message", exception.getMessage()));
    }
}
