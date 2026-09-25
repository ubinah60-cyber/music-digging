// 역할: 프로듀서 후보가 Artist 관계와 Recording 상세 관계에서 모두 확인되는지 검증한다.
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicBrainzProducerCandidateTest {

    private static final String PRODUCER_TYPE_ID =
            "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0";

    private final MusicBrainzApiClient client =
            mock(MusicBrainzApiClient.class);

    private final MusicBrainzRecordingParser parser =
            mock(MusicBrainzRecordingParser.class);

    private final MusicBrainzDataProvider provider =
            new MusicBrainzDataProvider(
                    client,
                    parser,
                    new ObjectMapper()
            );

    // Recording 상세에서도 같은 프로듀서가 확인된 곡만 반환한다.
    @Test
    void returnsOnlyVerifiedProducerRecordings() {
        TrackCredit producer = new TrackCredit(
                "producer-1",
                "프로듀서 A",
                CreditRole.PRODUCER
        );

        when(client.findArtistJson("producer-1"))
                .thenReturn(Optional.of("""
                        {
                          "relations": [
                            {
                              "target-type": "recording",
                              "type-id": "%s",
                              "recording": {"id": "recording-1"}
                            },
                            {
                              "target-type": "recording",
                              "type-id": "%s",
                              "recording": {"id": "recording-2"}
                            },
                            {
                              "target-type": "recording",
                              "type-id": "other-relation",
                              "recording": {"id": "recording-3"}
                            }
                          ]
                        }
                        """.formatted(
                        PRODUCER_TYPE_ID,
                        PRODUCER_TYPE_ID
                )));

        when(client.findRecordingJson("recording-1"))
                .thenReturn(Optional.of(recordingJson(
                        "recording-1",
                        "producer-1"
                )));

        when(client.findRecordingJson("recording-2"))
                .thenReturn(Optional.of(recordingJson(
                        "recording-2",
                        "different-producer"
                )));

        when(parser.parse(any(JsonNode.class)))
                .thenAnswer(invocation -> {
                    JsonNode recording = invocation.getArgument(0);

                    return new DigTrack(
                            recording.path("id").asText(),
                            "후보곡",
                            List.of()
                    );
                });

        List<DigTrack> result =
                provider.findTracksByCredit(producer);

        assertEquals(
                List.of("recording-1"),
                result.stream()
                        .map(DigTrack::recordingId)
                        .toList()
        );

        assertEquals(
                List.of(producer),
                result.get(0).credits()
        );

        verify(client, never())
                .findRecordingJson("recording-3");
    }

    // Artist가 조회되지 않으면 정상적인 후보 없음으로 숨기지 않는다.
    @Test
    void reportsMissingProducerArtist() {
        TrackCredit producer = new TrackCredit(
                "missing-producer",
                "없는 프로듀서",
                CreditRole.PRODUCER
        );

        when(client.findArtistJson("missing-producer"))
                .thenReturn(Optional.empty());

        assertThrows(
                MusicDataLookupException.class,
                () -> provider.findTracksByCredit(producer)
        );
    }

    // 테스트용 Recording과 Artist–Recording 프로듀서 관계를 만든다.
    private String recordingJson(
            String recordingId,
            String producerId
    ) {
        return """
                {
                  "id": "%s",
                  "relations": [
                    {
                      "target-type": "artist",
                      "type-id": "%s",
                      "artist": {
                        "id": "%s",
                        "name": "프로듀서"
                      },
                      "attributes": []
                    }
                  ]
                }
                """.formatted(
                recordingId,
                PRODUCER_TYPE_ID,
                producerId
        );
    }
}