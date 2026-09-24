package com.example.musicdigging.digging.service;

import com.example.musicdigging.digging.model.*;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import com.example.musicdigging.digging.provider.MusicDataProvider;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DiggingServiceTest {

    private final MusicDataProvider provider =
            mock(MusicDataProvider.class);

    private final DiggingService service =
            new DiggingService(provider);

    private final TrackCredit composer = new TrackCredit(
            "person-001", "작곡가 A", CreditRole.COMPOSER
    );

    private final DigTrack start = new DigTrack(
            "track-001", "시작곡", List.of(composer)
    );

    @Test
    void returnsConnectedCandidate() {
        DigTrack candidate = new DigTrack(
                "track-002", "후보곡", List.of(composer)
        );

        when(provider.findTrackById("track-001"))
                .thenReturn(Optional.of(start));

        when(provider.findTracksByCredit(composer))
                .thenReturn(List.of(start, candidate));

        DiggingResult result = service.dig("track-001");

        assertEquals("track-001", result.startRecordingId());
        assertEquals(DiggingStatus.SUCCESS, result.status());
        assertEquals(
                List.of(new DigCandidate(
                        "track-002", "후보곡", List.of(composer)
                )),
                result.candidates()
        );
    }

    @Test
    void distinguishesMissingTrack() {
        when(provider.findTrackById("missing"))
                .thenReturn(Optional.empty());

        DiggingResult result = service.dig("missing");

        assertEquals(DiggingStatus.TRACK_NOT_FOUND, result.status());
        assertTrue(result.candidates().isEmpty());
        verify(provider, never()).findTracksByCredit(any());
    }

    @Test
    void returnsNoCandidatesWhenAllCollectionMetadataIsMissing() {
        DigTrack noCredits = new DigTrack(
                "track-001", "시작곡", List.of()
        );

        when(provider.findTrackById("track-001"))
                .thenReturn(Optional.of(noCredits));

        DiggingResult result = service.dig("track-001");

        assertEquals(DiggingStatus.NO_CANDIDATES, result.status());
        assertTrue(result.candidates().isEmpty());
        verify(provider, never()).findTracksByCredit(any());
        verify(provider, never()).findTracksByArtist(anyString(), anyInt());
        verify(provider, never()).findTracksByGenreAndYearRange(anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void distinguishesNoCandidatesAfterExcludingStartTrack() {
        when(provider.findTrackById("track-001"))
                .thenReturn(Optional.of(start));

        when(provider.findTracksByCredit(composer))
                .thenReturn(List.of(start));

        DiggingResult result = service.dig("track-001");

        assertEquals(DiggingStatus.NO_CANDIDATES, result.status());
        assertTrue(result.candidates().isEmpty());
    }

    @Test
    void discoversGenreYearCandidateWithoutCreditsOrArtists() {
        DigTrack genreStart = new DigTrack("start", "시작곡", List.of(), List.of(),
                2020, List.of(" HIP HOP ", "hip hop"));
        DigTrack candidate = new DigTrack("candidate", "후보곡", List.of(), List.of(),
                2023, List.of("hip hop"));
        when(provider.findTrackById("start")).thenReturn(Optional.of(genreStart));
        when(provider.findTracksByGenreAndYearRange("hip hop", 2017, 2023, 30))
                .thenReturn(List.of(genreStart, candidate));

        DiggingResult result = service.dig("start");

        assertEquals(DiggingStatus.SUCCESS, result.status());
        assertEquals(List.of(new DigCandidate("candidate", "후보곡", List.of(),
                List.of(CandidateReason.genreAndYear("hip hop", 2017, 2023)))), result.candidates());
        verify(provider, never()).findTracksByCredit(any());
        verify(provider, never()).findTracksByArtist(anyString(), anyInt());
        verify(provider, times(1)).findTracksByGenreAndYearRange("hip hop", 2017, 2023, 30);
    }

    @Test
    void missingYearOnlySkipsGenreRouteAndArtistLookupIsDeduplicated() {
        DigTrack artistStart = new DigTrack("start", "시작곡", List.of(), List.of(
                new TrackArtist("a", "가수"), new TrackArtist("a", "Artist")),
                null, List.of("hip hop"));
        DigTrack candidate = new DigTrack("candidate", "후보곡", List.of(),
                List.of(new TrackArtist("a", "가수")), null, List.of());
        when(provider.findTrackById("start")).thenReturn(Optional.of(artistStart));
        when(provider.findTracksByArtist("a", 30)).thenReturn(List.of(candidate));

        DiggingResult result = service.dig("start");

        assertEquals(DiggingStatus.SUCCESS, result.status());
        assertEquals(List.of(CandidateReason.artist("a")), result.candidates().get(0).reasons());
        verify(provider, times(1)).findTracksByArtist("a", 30);
        verify(provider, never()).findTracksByGenreAndYearRange(anyString(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void propagatesStartTrackLookupFailure() {
        MusicDataLookupException failure =
                new MusicDataLookupException("시작곡 조회 실패");

        when(provider.findTrackById("track-001"))
                .thenThrow(failure);

        MusicDataLookupException thrown = assertThrows(
                MusicDataLookupException.class,
                () -> service.dig("track-001")
        );

        assertSame(failure, thrown);
        verify(provider, never()).findTracksByCredit(any());
    }

    @Test
    void propagatesCandidateLookupFailure() {
        when(provider.findTrackById("track-001"))
                .thenReturn(Optional.of(start));

        MusicDataLookupException failure =
                new MusicDataLookupException("참여곡 조회 실패");

        when(provider.findTracksByCredit(composer))
                .thenThrow(failure);

        MusicDataLookupException thrown = assertThrows(
                MusicDataLookupException.class,
                () -> service.dig("track-001")
        );

        assertSame(failure, thrown);
    }
}
