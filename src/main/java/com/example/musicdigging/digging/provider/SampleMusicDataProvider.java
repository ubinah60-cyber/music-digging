package com.example.musicdigging.digging.provider;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.model.TrackArtist;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

@Component
@Profile("local")
public class SampleMusicDataProvider implements MusicDataProvider {

    private final List<DigTrack> tracks;

    public SampleMusicDataProvider() {
        TrackCredit composerA = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        TrackCredit composerB = new TrackCredit(
                "person-002", "작곡가 B", CreditRole.COMPOSER
        );

        TrackArtist artistA = new TrackArtist("artist-001", "가수 A");
        TrackArtist artistB = new TrackArtist("artist-002", "가수 B");

        this.tracks = List.of(
                new DigTrack(
                        "track-001", "시작곡", List.of(composerA),
                        List.of(artistA), 2020, List.of("hip hop")
                ),
                new DigTrack(
                        "track-002", "세 경로가 겹치는 곡", List.of(composerA),
                        List.of(artistA), 2021, List.of("hip hop")
                ),
                new DigTrack(
                        "track-003", "장르와 연도로 연결된 곡", List.of(composerB),
                        List.of(artistB), 2017, List.of("hip hop")
                ),
                new DigTrack(
                        "track-004", "크레딧 없이 가수로 연결된 곡", List.of(),
                        List.of(artistA), 2000, List.of("rock")
                ),
                new DigTrack(
                        "track-005", "다른 시대의 같은 작곡가 곡", List.of(composerA),
                        List.of(artistB), 1990, List.of("jazz")
                ),
                new DigTrack(
                        "track-006", "연도 범위 밖의 다른 가수 곡", List.of(composerB),
                        List.of(artistB), 2016, List.of("hip hop")
                ),
                new DigTrack(
                        "track-007", "수집 정보가 없는 곡", List.of()
                )
        );
    }

    @Override
    public Optional<DigTrack> findTrackById(String recordingId) {
        for (DigTrack track : tracks) {
            if (track.recordingId().equals(recordingId)) {
                return Optional.of(track);
            }
        }

        return Optional.empty();
    }

    @Override
    public List<DigTrack> findTracksByCredit(TrackCredit credit) {
        List<DigTrack> matchedTracks = new ArrayList<>();

        for (DigTrack track : tracks) {
            for (TrackCredit trackCredit : track.credits()) {
                if (trackCredit.hasSamePersonAndRole(credit)) {
                    matchedTracks.add(track);
                    break;
                }
            }
        }

        return matchedTracks;
    }

    @Override
    public List<DigTrack> findTracksByArtist(String artistId, int limit) {
        validateLimit(limit);
        List<DigTrack> result = new ArrayList<>();
        for (DigTrack track : tracks) {
            for (TrackArtist artist : track.artists()) {
                if (artist.artistId().equals(artistId)) {
                    result.add(track);
                    break;
                }
            }
            if (result.size() == limit) {
                break;
            }
        }
        return result;
    }

    @Override
    public List<DigTrack> findTracksByGenreAndYearRange(
            String genre, int fromYear, int toYear, int limit
    ) {
        validateLimit(limit);
        if (genre == null || genre.isBlank() || fromYear < 1
                || toYear > 9999 || fromYear > toYear) {
            throw new IllegalArgumentException("장르와 유효한 연도 범위가 필요합니다.");
        }
        String normalizedGenre = genre.strip().toLowerCase(Locale.ROOT);
        List<DigTrack> result = new ArrayList<>();
        for (DigTrack track : tracks) {
            Integer year = track.firstReleaseYear();
            if (year != null && year >= fromYear && year <= toYear
                    && track.genres().contains(normalizedGenre)) {
                result.add(track);
                if (result.size() == limit) {
                    break;
                }
            }
        }
        return result;
    }

    private void validateLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("조회 상한은 양수여야 합니다.");
        }
    }
}
