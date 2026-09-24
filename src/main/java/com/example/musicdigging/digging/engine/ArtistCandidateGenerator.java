package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.*;

import java.util.ArrayList;
import java.util.List;

public class ArtistCandidateGenerator {

    public List<DigCandidate> generate(DigTrack startTrack, List<DigTrack> tracks) {
        List<DigCandidate> candidates = new ArrayList<>();
        for (DigTrack track : tracks) {
            if (startTrack.recordingId().equals(track.recordingId())) {
                continue;
            }
            List<CandidateReason> reasons = new ArrayList<>();
            for (TrackArtist startArtist : startTrack.artists()) {
                for (TrackArtist artist : track.artists()) {
                    if (startArtist.artistId().equals(artist.artistId())) {
                        CandidateReason reason = CandidateReason.artist(artist.artistId());
                        if (!reasons.contains(reason)) {
                            reasons.add(reason);
                        }
                        break;
                    }
                }
            }
            if (!reasons.isEmpty()) {
                candidates.add(new DigCandidate(track.recordingId(), track.title(), List.of(), reasons));
            }
        }
        return candidates;
    }
}
