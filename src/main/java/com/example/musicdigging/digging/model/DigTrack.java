package com.example.musicdigging.digging.model;

import java.util.List;

public record DigTrack(
        String recordingId,
        String title,
        List<TrackCredit> credits
) {
    public DigTrack {
        if (recordingId == null || recordingId.isBlank()) {
            throw new IllegalArgumentException("곡 ID는 필수입니다.");
        }

        credits = List.copyOf(credits);
    }
}