package com.example.musicdigging.service;

import com.example.musicdigging.domain.Music;
import com.example.musicdigging.mapper.MusicMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicService {

    private final MusicMapper musicMapper;

    public List<Music> findAll() {
        return musicMapper.findAll();
    }

    public List<Music> search(String keyword) {
        return musicMapper.search(keyword);
    }
}