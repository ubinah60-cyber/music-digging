package com.example.musicdigging.digging.model;

import java.util.List;

public record RecordingWorkLink(
        String provider,
        String recordingId,
        String workId,
        String relationshipTypeId,
        List<String> attributes
) {
    public RecordingWorkLink {
        attributes = List.copyOf(attributes);
    }
}