/** Recording JSON이 DigTrack으로 정확히 변환되는지 검증한다. */
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class MusicBrainzRecordingParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MusicBrainzRecordingParser parser =
            new MusicBrainzRecordingParser();

    /** Recording에 연결된 프로듀서를 곡 크레딧으로 변환한다. */
    @Test
    void parsesRecordingAndProducer() {
        String json = """
                {
                  "id": "recording-001",
                  "title": "시작곡",
                  "artist-credit": [
                    {"artist": {"id": "artist-001", "name": "Queen"}},
                    " feat. ",
                    {"artist": {"id": "artist-002", "name": "Guest"}}
                  ],
                  "genres": [
                    {"name": "Rock", "count": 10},
                    {"name": "Hard Rock", "count": 4}
                  ],
                  "relations": [
                    {
                      "target-type": "artist",
                      "type-id": "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0",
                      "artist": {
                        "id": "person-001",
                        "name": "프로듀서 A"
                      },
                      "attributes": []
                    }
                  ]
                }
                """;

        DigTrack track = parser.parse(objectMapper.readTree(json));

        assertEquals("recording-001", track.recordingId());
        assertEquals("시작곡", track.title());
        assertEquals(1, track.credits().size());
        assertEquals("person-001", track.credits().get(0).personId());
        assertEquals(CreditRole.PRODUCER, track.credits().get(0).role());
        assertEquals(2, track.artists().size());
        assertEquals("Queen", track.artists().get(0).name());
        assertEquals("artist-002", track.artists().get(1).artistId());
        assertEquals(List.of("rock", "hard rock"), track.genres());
    }

    /** 필수 제목이 빠진 응답을 정상 곡으로 취급하지 않는다. */
    @Test
    void rejectsRecordingWithoutTitle() {
        String json = """
                {
                  "id": "recording-001",
                  "relations": []
                }
                """;

        assertThrows(
                MusicDataLookupException.class,
                () -> parser.parse(objectMapper.readTree(json))
        );
    }
}