package com.example.musicdigging.digging.model;

import java.util.List;

/*이 곡이 다음 디깅 후보이며, 왜 후보가 되었는가*/

public record DigCandidate(
        String recordingId,
        String title,
        List<TrackCredit> connections
) {
    public DigCandidate {
        connections = List.copyOf(connections);
    }
}