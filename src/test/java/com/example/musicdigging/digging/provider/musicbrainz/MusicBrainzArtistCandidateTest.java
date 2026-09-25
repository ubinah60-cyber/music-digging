// 역할: 아티스트 후보의 ID 필터링, 페이지 분산, 잘못된 응답 처리를 검증한다.
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackArtist;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MusicBrainzArtistCandidateTest {

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

    // 요청한 아티스트가 실제 참여한 Recording만 반환한다.
    @Test
    void returnsOnlyRecordingsCreditedToRequestedArtist() {
        String artistId = "queen-id";

        when(client.browseRecordingsByArtistJson(artistId, 3))
                .thenReturn("""
                        {
                          "recordings": [
                            {"id": "recording-1"},
                            {"id": "recording-2"},
                            {"id": "recording-3"}
                          ]
                        }
                        """);

        when(parser.parse(any(JsonNode.class)))
                .thenAnswer(invocation -> {
                    JsonNode recording = invocation.getArgument(0);
                    String recordingId =
                            recording.path("id").asText();

                    String creditedArtistId =
                            recordingId.equals("recording-2")
                                    ? "other-artist-id"
                                    : artistId;

                    return new DigTrack(
                            recordingId,
                            "테스트 곡 " + recordingId,
                            List.of(),
                            List.of(new TrackArtist(
                                    creditedArtistId,
                                    "테스트 아티스트"
                            )),
                            null,
                            List.of()
                    );
                });

        List<DigTrack> result =
                provider.findTracksByArtist(artistId, 3);

        assertEquals(
                List.of("recording-1", "recording-3"),
                result.stream()
                        .map(DigTrack::recordingId)
                        .toList()
        );

        verify(client).browseRecordingsByArtistJson(artistId, 3);
    }

    // 앞·중간·뒤 페이지에서 제목이 다른 후보를 수집한다.
    @Test
    void collectsDifferentTitlesAcrossThreePages() {
        String artistId = "queen-id";

        when(client.browseRecordingsByArtistJson(artistId, 3))
                .thenReturn("""
                        {
                          "recording-count": 90,
                          "recordings": [
                            {"id": "a-1"},
                            {"id": "a-2"},
                            {"id": "a-3"}
                          ]
                        }
                        """);

        when(client.browseRecordingsByArtistJson(
                artistId, 3, 43
        )).thenReturn("""
                {
                  "recordings": [
                    {"id": "m-1"},
                    {"id": "m-2"},
                    {"id": "m-3"}
                  ]
                }
                """);

        when(client.browseRecordingsByArtistJson(
                artistId, 3, 87
        )).thenReturn("""
                {
                  "recordings": [
                    {"id": "z-1"},
                    {"id": "z-2"},
                    {"id": "z-3"}
                  ]
                }
                """);

        when(parser.parse(any(JsonNode.class)))
                .thenAnswer(invocation -> {
                    JsonNode recording = invocation.getArgument(0);
                    String recordingId =
                            recording.path("id").asText();

                    String title;
                    if (recordingId.startsWith("a-")) {
                        title = "’39";
                    } else if (recordingId.startsWith("m-")) {
                        title = "중간 구간 곡";
                    } else {
                        title = "마지막 구간 곡";
                    }

                    return new DigTrack(
                            recordingId,
                            title,
                            List.of(),
                            List.of(new TrackArtist(
                                    artistId,
                                    "Queen"
                            )),
                            null,
                            List.of()
                    );
                });

        List<DigTrack> result =
                provider.findTracksByArtist(artistId, 3);

        assertEquals(
                List.of("’39", "중간 구간 곡", "마지막 구간 곡"),
                result.stream()
                        .map(DigTrack::title)
                        .toList()
        );

        verify(client).browseRecordingsByArtistJson(artistId, 3);
        verify(client).browseRecordingsByArtistJson(artistId, 3, 43);
        verify(client).browseRecordingsByArtistJson(artistId, 3, 87);
    }

    // recordings 배열이 없으면 빈 후보 대신 조회 오류를 낸다.
    @Test
    void rejectsResponseWithoutRecordingsArray() {
        when(client.browseRecordingsByArtistJson("queen-id", 3))
                .thenReturn("{}");

        assertThrows(
                MusicDataLookupException.class,
                () -> provider.findTracksByArtist("queen-id", 3)
        );
    }
}