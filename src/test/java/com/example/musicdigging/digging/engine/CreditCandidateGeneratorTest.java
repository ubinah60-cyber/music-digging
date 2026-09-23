package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigCandidate;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CreditCandidateGeneratorTest {

    private final CreditCandidateGenerator generator =
            new CreditCandidateGenerator();

    @Test
    void excludesStartTrackAndUnrelatedTracks() {
        TrackCredit composer = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        DigTrack start = new DigTrack(
                "track-001", "시작곡", List.of(composer)
        );

        DigTrack connected = new DigTrack(
                "track-002", "연결된 곡", List.of(composer)
        );

        DigTrack unrelated = new DigTrack(
                "track-003",
                "관련 없는 곡",
                List.of(new TrackCredit(
                        "person-999", "작곡가 B", CreditRole.COMPOSER
                ))
        );

        List<DigCandidate> result = generator.generate(
                start, List.of(start, connected, unrelated)
        );

        assertEquals(
                List.of(new DigCandidate(
                        "track-002", "연결된 곡", List.of(composer)
                )),
                result
        );
    }

    @Test
    void mergesConnectionsForDuplicateTrackWithoutRepeatingReasons() {
        TrackCredit composer = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        TrackCredit producer = new TrackCredit(
                "person-002", "프로듀서 B", CreditRole.PRODUCER
        );

        DigTrack start = new DigTrack(
                "track-001", "시작곡", List.of(composer, producer)
        );

        DigTrack foundByComposer = new DigTrack(
                "track-002", "후보곡", List.of(composer)
        );

        DigTrack foundByProducer = new DigTrack(
                "track-002", "후보곡", List.of(producer)
        );

        List<DigCandidate> result = generator.generate(
                start,
                List.of(
                        foundByComposer,
                        foundByProducer,
                        foundByComposer
                )
        );

        assertEquals(1, result.size());
        assertEquals("track-002", result.get(0).recordingId());
        assertEquals(
                List.of(composer, producer),
                result.get(0).connections()
        );
    }
}