package com.example.musicdigging.digging.model;

import java.util.List;

public record DigCandidate(
        String recordingId,
        String title,
        List<TrackCredit> connections
) {
    public DigCandidate {
        connections = List.copyOf(connections);
    }
}