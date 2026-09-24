package com.example.musicdigging.digging.service;

import com.example.musicdigging.digging.model.*;
import com.example.musicdigging.digging.provider.SampleMusicDataProvider;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiRouteDiggingServiceTest {
    private final DiggingService service = new DiggingService(new SampleMusicDataProvider());

    @Test
    void collectsAllRoutesEvenWhenCreditCandidatesExist() {
        DiggingResult result = service.dig("track-001");
        assertEquals(DiggingStatus.SUCCESS, result.status());
        assertEquals(List.of("track-002", "track-005", "track-004", "track-003"),
                result.candidates().stream().map(DigCandidate::recordingId).toList());
        DigCandidate shared = result.candidates().get(0);
        assertEquals(List.of(CandidateReasonType.CREDIT, CandidateReasonType.SAME_ARTIST,
                        CandidateReasonType.GENRE_AND_YEAR),
                shared.reasons().stream().map(CandidateReason::type).toList());
        // 연도 범위는 장르 경로에만 적용하므로 1990년 크레딧 후보도 남는다.
        assertTrue(result.candidates().stream().anyMatch(c -> c.recordingId().equals("track-005")));
    }

    @Test
    void creditlessStartCanStillDiscoverArtistCandidates() {
        DiggingResult result = service.dig("track-004");
        assertEquals(DiggingStatus.SUCCESS, result.status());
        assertEquals(List.of("track-001", "track-002"),
                result.candidates().stream().map(DigCandidate::recordingId).toList());
        for (DigCandidate candidate : result.candidates()) {
            assertTrue(candidate.connections().isEmpty());
            assertEquals(List.of(CandidateReason.artist("artist-001")), candidate.reasons());
        }
    }

    @Test
    void missingMetadataAndUnknownTracksRemainDistinct() {
        assertEquals(DiggingStatus.NO_CANDIDATES, service.dig("track-007").status());
        assertEquals(DiggingStatus.TRACK_NOT_FOUND, service.dig("missing").status());
        assertThrows(IllegalArgumentException.class, () -> service.dig(" "));
    }
}
