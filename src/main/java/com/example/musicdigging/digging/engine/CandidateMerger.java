package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class CandidateMerger {

    public List<DigCandidate> merge(String startRecordingId, List<DigCandidate> candidates) {
        Map<String, DigCandidate> merged = new LinkedHashMap<>();
        for (DigCandidate candidate : candidates) {
            if (startRecordingId.equals(candidate.recordingId())) {
                continue;
            }
            DigCandidate existing = merged.get(candidate.recordingId());
            List<TrackCredit> connections = new ArrayList<>();
            LinkedHashSet<CandidateReason> reasons = new LinkedHashSet<>();
            if (existing != null) {
                connections.addAll(existing.connections());
                reasons.addAll(existing.reasons());
            }
            for (TrackCredit credit : candidate.connections()) {
                boolean alreadyIncluded = connections.stream()
                        .anyMatch(saved -> saved.hasSamePersonAndRole(credit));
                if (!alreadyIncluded) {
                    connections.add(credit);
                }
            }
            reasons.addAll(candidate.reasons());
            merged.put(candidate.recordingId(), new DigCandidate(
                    candidate.recordingId(),
                    existing == null ? candidate.title() : existing.title(),
                    connections, new ArrayList<>(reasons)));
        }
        return List.copyOf(merged.values());
    }
}
