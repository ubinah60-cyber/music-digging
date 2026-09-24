package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.*;

import java.util.ArrayList;
import java.util.List;

public class GenreYearCandidateGenerator {

    public List<DigCandidate> generate(
            DigTrack startTrack, List<DigTrack> tracks, int fromYear, int toYear
    ) {
        if (fromYear < 1 || toYear > 9999 || fromYear > toYear) {
            throw new IllegalArgumentException("유효한 연도 범위가 필요합니다.");
        }
        if (startTrack.firstReleaseYear() == null || startTrack.genres().isEmpty()) {
            return List.of();
        }
        List<DigCandidate> candidates = new ArrayList<>();
        for (DigTrack track : tracks) {
            Integer year = track.firstReleaseYear();
            if (startTrack.recordingId().equals(track.recordingId())
                    || year == null || year < fromYear || year > toYear) {
                continue;
            }
            List<CandidateReason> reasons = new ArrayList<>();
            for (String genre : startTrack.genres()) {
                if (track.genres().contains(genre)) {
                    reasons.add(CandidateReason.genreAndYear(genre, fromYear, toYear));
                }
            }
            if (!reasons.isEmpty()) {
                candidates.add(new DigCandidate(track.recordingId(), track.title(), List.of(), reasons));
            }
        }
        return candidates;
    }
}
