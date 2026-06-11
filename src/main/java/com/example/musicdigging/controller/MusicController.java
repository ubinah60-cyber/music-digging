package com.example.musicdigging.controller;

import com.example.musicdigging.domain.Music;
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

    @GetMapping("/api/music/list")
    public List<Music> list() {
        return musicService.findAll();
    }

    @GetMapping("/api/music/search")
    public List<Music> search(@RequestParam String keyword) {
        return musicService.search(keyword);
    }
}