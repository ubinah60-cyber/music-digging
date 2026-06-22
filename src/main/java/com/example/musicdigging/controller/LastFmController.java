package com.example.musicdigging.controller;

import com.example.musicdigging.dto.SimilarArtistDto;
import com.example.musicdigging.external.lastfm.LastFmService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class LastFmController {

    private final LastFmService lastFmService;

    @GetMapping("/api/lastfm/similar-artists")
    public List<SimilarArtistDto> getSimilarArtists(
            @RequestParam String artist
    ) {
        return lastFmService.getSimilarArtists(artist);
    }
}