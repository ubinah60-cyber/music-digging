package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreditConnectionFinderTest {

    private final CreditConnectionFinder finder =
            new CreditConnectionFinder();

    @Test
    void findsSharedComposerBetweenDifferentTracks() {
        TrackCredit composer = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        DigTrack start = new DigTrack(
                "track-001", "시작곡", List.of(composer)
        );

        DigTrack candidate = new DigTrack(
                "track-002",
                "후보곡",
                List.of(
                        new TrackCredit(
                                "person-001",
                                "Composer A",
                                CreditRole.COMPOSER
                        ),
                        new TrackCredit(
                                "person-002",
                                "프로듀서 B",
                                CreditRole.PRODUCER
                        )
                )
        );

        List<TrackCredit> result =
                finder.findConnections(start, candidate);

        assertEquals(List.of(composer), result);
    }

    @Test
    void excludesSameRecordingEvenWhenCreditsMatch() {
        TrackCredit composer = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        DigTrack start = new DigTrack(
                "track-001", "시작곡", List.of(composer)
        );

        DigTrack candidate = new DigTrack(
                "track-001", "같은 곡의 다른 표기", List.of(composer)
        );

        assertTrue(finder.findConnections(start, candidate).isEmpty());
    }

    @Test
    void returnsEmptyWhenCandidateHasNoCredits() {
        DigTrack start = new DigTrack(
                "track-001",
                "시작곡",
                List.of(new TrackCredit(
                        "person-001", "작곡가 A", CreditRole.COMPOSER
                ))
        );

        DigTrack candidate = new DigTrack(
                "track-002", "크레딧 없는 곡", List.of()
        );

        assertTrue(finder.findConnections(start, candidate).isEmpty());
    }
}