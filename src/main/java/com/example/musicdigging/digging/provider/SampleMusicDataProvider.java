package com.example.musicdigging.digging.provider;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        this.tracks = List.of(
                new DigTrack(
                        "track-001", "시작곡", List.of(composerA)
                ),
                new DigTrack(
                        "track-002", "연결된 후보곡", List.of(composerA)
                ),
                new DigTrack(
                        "track-003", "다른 작곡가의 곡", List.of(composerB)
                ),
                new DigTrack(
                        "track-004", "크레딧 없는 곡", List.of()
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
}