package com.example.musicdigging.controller;

import com.example.musicdigging.domain.Music;
import com.example.musicdigging.dto.*;
import com.example.musicdigging.external.music.MusicBrainzService;
import com.example.musicdigging.service.MusicService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.util.List;

@RestController
@RequiredArgsConstructor
public class MusicController {

    private final MusicService musicService;
    private final MusicBrainzService musicBrainzService;

    @GetMapping("/api/music/list")
    public List<Music> list() {
        return musicService.findAll();
    }

    @GetMapping("/api/music/search")
    public List<Music> search(@RequestParam String keyword) {
        return musicService.search(keyword);
    }

    @GetMapping("/api/music/artist")
    public List<ArtistDto> artist(@RequestParam String name) {

        return musicBrainzService.searchArtist(name);
    }

    @GetMapping("/api/music/albums")
    public List<AlbumDto> albums(@RequestParam String artistName) {
        return musicBrainzService.searchAlbums(artistName);
    }

    @GetMapping("/api/music/album-detail")
    public AlbumDto albumDetail(@RequestParam String id) {
        return musicBrainzService.getAlbumDetail(id);
    }

    @GetMapping("/api/music/tracks")
    public List<TrackDto> tracks(@RequestParam String releaseId) {
        return musicBrainzService.getTracks(releaseId);
    }

    @GetMapping("/api/music/track-credit")
    public List<CreditDto> getTrackCredit(
            @RequestParam String recordingId
    ) {
        return musicBrainzService
                .getTrackCredits(recordingId);
    }

    @GetMapping("/api/music/credit-recommend")
    public List<RecommendationDto> creditRecommend(@RequestParam String name) {
        return musicBrainzService.getRecommendationsByCredit(name);
    }
}