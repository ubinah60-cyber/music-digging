package com.example.musicdigging.digging.service;

import com.example.musicdigging.digging.engine.CreditCandidateGenerator;
import com.example.musicdigging.digging.model.*;
import com.example.musicdigging.digging.provider.MusicDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiggingService {

    private final MusicDataProvider provider;

    private final CreditCandidateGenerator generator =
            new CreditCandidateGenerator();

    public DiggingService(MusicDataProvider provider) {
        this.provider = provider;
    }

    public DiggingResult dig(String recordingId) {
        if (recordingId == null || recordingId.isBlank()) {
            throw new IllegalArgumentException("시작곡 ID는 필수입니다.");
        }

        Optional<DigTrack> foundTrack =
                provider.findTrackById(recordingId);

        if (foundTrack.isEmpty()) {
            return new DiggingResult(
                    recordingId,
                    DiggingStatus.TRACK_NOT_FOUND,
                    List.of()
            );
        }

        DigTrack startTrack = foundTrack.get();

        if (startTrack.credits().isEmpty()) {
            return new DiggingResult(
                    recordingId,
                    DiggingStatus.NO_CREDITS,
                    List.of()
            );
        }

        List<DigTrack> collectedTracks = new ArrayList<>();

        for (TrackCredit credit : startTrack.credits()) {
            List<DigTrack> tracks =
                    provider.findTracksByCredit(credit);

            collectedTracks.addAll(tracks);
        }

        List<DigCandidate> candidates =
                generator.generate(startTrack, collectedTracks);

        DiggingStatus status = candidates.isEmpty()
                ? DiggingStatus.NO_CANDIDATES
                : DiggingStatus.SUCCESS;

        return new DiggingResult(recordingId, status, candidates);
    }
}