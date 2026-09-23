package com.example.musicdigging.digging.model;

import java.util.List;

public record DiggingResult(
        String startRecordingId,
        DiggingStatus status,
        List<DigCandidate> candidates
) {
    public DiggingResult {
        candidates = List.copyOf(candidates);
    }
}