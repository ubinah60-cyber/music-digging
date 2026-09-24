package com.example.musicdigging.digging.service;

import com.example.musicdigging.digging.engine.*;
import com.example.musicdigging.digging.model.*;
import com.example.musicdigging.digging.provider.MusicDataProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DiggingService {

    // 수집 정책이다. 추천 순위와 세션당 최종 곡 수는 이후 단계에서 결정한다.
    private static final int YEAR_RADIUS = 3;
    private static final int LOOKUP_LIMIT = 30;

    private final MusicDataProvider provider;

    private final CreditCandidateGenerator generator =
            new CreditCandidateGenerator();
    private final ArtistCandidateGenerator artistGenerator = new ArtistCandidateGenerator();
    private final GenreYearCandidateGenerator genreYearGenerator = new GenreYearCandidateGenerator();
    private final CandidateMerger merger = new CandidateMerger();

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

        List<DigTrack> collectedTracks = new ArrayList<>();

        for (TrackCredit credit : startTrack.credits()) {
            List<DigTrack> tracks =
                    provider.findTracksByCredit(credit);

            collectedTracks.addAll(tracks);
        }

        List<DigCandidate> collectedCandidates = new ArrayList<>(
                generator.generate(startTrack, collectedTracks));

        // 크레딧 유무와 관계없이 다른 수집 경로도 실행한다.
        List<String> artistIds = startTrack.artists().stream()
                .map(TrackArtist::artistId).distinct().toList();
        for (String artistId : artistIds) {
            List<DigTrack> tracks = provider.findTracksByArtist(artistId, LOOKUP_LIMIT);
            collectedCandidates.addAll(artistGenerator.generate(startTrack, tracks));
        }

        if (startTrack.firstReleaseYear() != null && !startTrack.genres().isEmpty()) {
            int fromYear = Math.max(1, startTrack.firstReleaseYear() - YEAR_RADIUS);
            int toYear = Math.min(9999, startTrack.firstReleaseYear() + YEAR_RADIUS);
            for (String genre : startTrack.genres()) {
                List<DigTrack> tracks = provider.findTracksByGenreAndYearRange(
                        genre, fromYear, toYear, LOOKUP_LIMIT);
                collectedCandidates.addAll(
                        genreYearGenerator.generate(startTrack, tracks, fromYear, toYear));
            }
        }

        List<DigCandidate> candidates = merger.merge(recordingId, collectedCandidates);

        DiggingStatus status = candidates.isEmpty()
                ? DiggingStatus.NO_CANDIDATES
                : DiggingStatus.SUCCESS;

        return new DiggingResult(recordingId, status, candidates);
    }
}
