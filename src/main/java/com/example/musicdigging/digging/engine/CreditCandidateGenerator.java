package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.DigCandidate;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreditCandidateGenerator {

    private final CreditConnectionFinder connectionFinder =
            new CreditConnectionFinder();

    public List<DigCandidate> generate(
            DigTrack startTrack,
            List<DigTrack> candidateTracks
    ) {
        Map<String, DigCandidate> candidatesById = new LinkedHashMap<>();

        for (DigTrack track : candidateTracks) {
            List<TrackCredit> connections =
                    connectionFinder.findConnections(startTrack, track);

            if (connections.isEmpty()) {
                continue;
            }

            DigCandidate existing =
                    candidatesById.get(track.recordingId());

            List<TrackCredit> mergedConnections = new ArrayList<>();

            if (existing != null) {
                mergedConnections.addAll(existing.connections());
            }

            for (TrackCredit connection : connections) {
                boolean alreadyIncluded = false;

                for (TrackCredit saved : mergedConnections) {
                    if (saved.hasSamePersonAndRole(connection)) {
                        alreadyIncluded = true;
                        break;
                    }
                }

                if (!alreadyIncluded) {
                    mergedConnections.add(connection);
                }
            }

            DigCandidate candidate = new DigCandidate(
                    track.recordingId(),
                    existing == null ? track.title() : existing.title(),
                    mergedConnections
            );

            candidatesById.put(track.recordingId(), candidate);
        }

        return new ArrayList<>(candidatesById.values());
    }
}