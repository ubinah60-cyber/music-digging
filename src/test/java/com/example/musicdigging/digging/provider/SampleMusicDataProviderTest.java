package com.example.musicdigging.digging.provider;

import com.example.musicdigging.digging.model.DigTrack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SampleMusicDataProviderTest {
    private final SampleMusicDataProvider provider = new SampleMusicDataProvider();

    @Test
    void artistLookupHonorsLimit() {
        assertEquals(List.of("track-001", "track-002", "track-004"),
                provider.findTracksByArtist("artist-001", 30).stream().map(DigTrack::recordingId).toList());
        assertEquals(1, provider.findTracksByArtist("artist-001", 1).size());
        assertTrue(provider.findTracksByArtist("missing", 30).isEmpty());
    }

    @Test
    void genreYearLookupNormalizesGenreAndHonorsLimitAndBounds() {
        assertEquals(List.of("track-001", "track-002", "track-003"),
                provider.findTracksByGenreAndYearRange(" HIP HOP ", 2017, 2023, 30)
                        .stream().map(DigTrack::recordingId).toList());
        assertEquals(1, provider.findTracksByGenreAndYearRange("hip hop", 2017, 2023, 1).size());
        assertTrue(provider.findTracksByGenreAndYearRange("rock", 2017, 2023, 30).isEmpty());
    }

    @Test
    void rejectsInvalidQueryBounds() {
        assertThrows(IllegalArgumentException.class, () -> provider.findTracksByArtist("artist-001", 0));
        assertThrows(IllegalArgumentException.class,
                () -> provider.findTracksByGenreAndYearRange("hip hop", 2023, 2017, 30));
        assertThrows(IllegalArgumentException.class,
                () -> provider.findTracksByGenreAndYearRange("hip hop", 2017, 2023, -1));
    }
}
