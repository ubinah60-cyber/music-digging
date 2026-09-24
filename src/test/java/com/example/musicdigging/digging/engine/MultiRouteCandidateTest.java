package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MultiRouteCandidateTest {
    private final TrackArtist artist = new TrackArtist("artist-a", "가수 A");
    private final DigTrack start = track("start", List.of(artist), 2020, List.of("hip hop"));

    private DigTrack track(String id, List<TrackArtist> artists, Integer year, List<String> genres) {
        return new DigTrack(id, id, List.of(), artists, year, genres);
    }

    @Test
    void artistUsesIdAndSupportsCollaborationsWithoutCredits() {
        DigTrack collaboration = track("collab", List.of(
                new TrackArtist("other", "다른 가수"),
                new TrackArtist("artist-a", "다른 표기")), null, List.of());
        DigTrack namesake = track("namesake", List.of(new TrackArtist("different-id", "가수 A")),
                2020, List.of());
        List<DigCandidate> result = new ArtistCandidateGenerator()
                .generate(start, List.of(start, collaboration, namesake));
        assertEquals(List.of("collab"), result.stream().map(DigCandidate::recordingId).toList());
        assertTrue(result.get(0).connections().isEmpty());
        assertEquals(List.of(CandidateReason.artist("artist-a")), result.get(0).reasons());
    }

    @Test
    void genreRequiresBothMatchingGenreAndInclusiveYearRange() {
        List<DigTrack> tracks = List.of(start,
                track("lower", List.of(), 2017, List.of(" HIP HOP ")),
                track("upper", List.of(), 2023, List.of("hip hop")),
                track("early", List.of(), 2016, List.of("hip hop")),
                track("late", List.of(), 2024, List.of("hip hop")),
                track("wrong-genre", List.of(), 2020, List.of("rock")),
                track("no-year", List.of(), null, List.of("hip hop")),
                track("no-genre", List.of(), 2020, List.of()));
        List<DigCandidate> result = new GenreYearCandidateGenerator().generate(start, tracks, 2017, 2023);
        assertEquals(List.of("lower", "upper"), result.stream().map(DigCandidate::recordingId).toList());
        assertEquals(List.of(CandidateReason.genreAndYear("hip hop", 2017, 2023)), result.get(0).reasons());
    }

    @Test
    void missingStartMetadataSkipsGenreYearRoute() {
        GenreYearCandidateGenerator generator = new GenreYearCandidateGenerator();
        assertTrue(generator.generate(track("s", List.of(), null, List.of("hip hop")),
                List.of(start), 2017, 2023).isEmpty());
        assertTrue(generator.generate(track("s", List.of(), 2020, List.of()),
                List.of(start), 2017, 2023).isEmpty());
    }

    @Test
    void mergerPreservesAllReasonsAndDeduplicatesCreditsByPersonAndRole() {
        TrackCredit composer = new TrackCredit("p", "작곡가", CreditRole.COMPOSER);
        TrackCredit alternateName = new TrackCredit("p", "Composer", CreditRole.COMPOSER);
        TrackCredit producer = new TrackCredit("p", "작곡가", CreditRole.PRODUCER);
        DigCandidate credit = new DigCandidate("song", "제목", List.of(composer));
        DigCandidate moreCredits = new DigCandidate("song", "다른 표기", List.of(alternateName, producer));
        DigCandidate byArtist = new DigCandidate("song", "제목", List.of(),
                List.of(CandidateReason.artist("artist-a")));
        DigCandidate byGenre = new DigCandidate("song", "제목", List.of(),
                List.of(CandidateReason.genreAndYear("hip hop", 2017, 2023)));
        List<DigCandidate> result = new CandidateMerger().merge("start", List.of(
                new DigCandidate("start", "시작곡", List.of(composer)),
                credit, moreCredits, byArtist, byGenre, byArtist));
        assertEquals(1, result.size());
        assertEquals("제목", result.get(0).title());
        assertEquals(List.of(composer, producer), result.get(0).connections());
        assertEquals(List.of(CandidateReason.credit(composer), CandidateReason.credit(producer),
                CandidateReason.artist("artist-a"), CandidateReason.genreAndYear("hip hop", 2017, 2023)),
                result.get(0).reasons());
    }

    @Test
    void metadataCollectionsAreImmutableAndGenresAreNormalized() {
        List<TrackArtist> artists = new ArrayList<>(List.of(artist));
        List<String> genres = new ArrayList<>(List.of(" HIP HOP ", "hip hop", " "));
        DigTrack track = track("song", artists, null, genres);
        artists.clear();
        genres.clear();
        assertEquals(List.of(artist), track.artists());
        assertEquals(List.of("hip hop"), track.genres());
        assertThrows(UnsupportedOperationException.class, () -> track.genres().add("rock"));
        assertThrows(IllegalArgumentException.class, () -> track("bad", List.of(), 0, List.of()));
    }
}
