package com.example.musicdigging.digging.model;

public record TrackArtist(String artistId, String name) {
    public TrackArtist {
        if (artistId == null || artistId.isBlank()) {
            throw new IllegalArgumentException("가수 ID는 필수입니다.");
        }
    }
}
