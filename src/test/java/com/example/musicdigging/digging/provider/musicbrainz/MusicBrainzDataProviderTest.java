/** MusicBrainz 응답이 디깅 곡 데이터와 오류 상태로 전달되는지 검증한다. */
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.DiggingStatus;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import com.example.musicdigging.digging.service.DiggingService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MusicBrainzDataProviderTest {

    /** Recording의 프로듀서와 연결된 Work의 작곡가·작사가를 합친다. */
    @Test
    void mergesRecordingAndWorkCredits() {
        String recordingJson = """
                {
                  "id": "recording-001",
                  "title": "테스트곡",
                  "artist-credit": [
                    {"artist": {"id": "artist-001", "name": "가수 A"}}
                  ],
                  "genres": [{"name": "Rock"}],
                  "relations": [
                    {
                      "target-type": "artist",
                      "type-id": "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0",
                      "artist": {
                        "id": "person-001",
                        "name": "프로듀서 A"
                      },
                      "attributes": []
                    },
                    {
                      "target-type": "work",
                      "type-id": "a3005666-a872-32c3-ad06-98af558e99b0",
                      "work": {"id": "work-001"},
                      "attributes": []
                    }
                  ]
                }
                """;

        String workJson = """
                {
                  "id": "work-001",
                  "relations": [
                    {
                      "target-type": "artist",
                      "type-id": "d59d99ea-23d4-4a80-b066-edca32ee158f",
                      "artist": {
                        "id": "person-002",
                        "name": "작곡가 B"
                      },
                      "attributes": []
                    },
                    {
                      "target-type": "artist",
                      "type-id": "3e48faba-ec01-47fd-8e89-30e81161661c",
                      "artist": {
                        "id": "person-003",
                        "name": "작사가 C"
                      },
                      "attributes": []
                    }
                  ]
                }
                """;

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        server.expect(request -> assertEquals(
                        "/ws/2/recording/recording-001",
                        request.getURI().getPath()
                ))
                .andRespond(withSuccess(
                        recordingJson, MediaType.APPLICATION_JSON
                ));

        server.expect(request -> assertEquals(
                        "/ws/2/work/work-001",
                        request.getURI().getPath()
                ))
                .andRespond(withSuccess(
                        workJson, MediaType.APPLICATION_JSON
                ));

        DigTrack track = newProvider(builder)
                .findTrackById("recording-001")
                .orElseThrow();

        assertEquals("테스트곡", track.title());
        assertEquals("가수 A", track.artists().get(0).name());
        assertEquals(List.of("rock"), track.genres());
        assertEquals(
                List.of(
                        CreditRole.PRODUCER,
                        CreditRole.COMPOSER,
                        CreditRole.LYRICIST
                ),
                track.credits().stream()
                        .map(TrackCredit::role)
                        .toList()
        );
        assertEquals("person-002", track.credits().get(1).personId());
        server.verify();
    }

    /** MusicBrainz의 404를 시작곡 없음 상태로 전달한다. */
    @Test
    void returnsTrackNotFoundForUnknownRecording() {
        String recordingId = "00000000-0000-0000-0000-000000000000";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        server.expect(request -> assertEquals(
                        "/ws/2/recording/" + recordingId,
                        request.getURI().getPath()
                ))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        var result = new DiggingService(newProvider(builder))
                .dig(recordingId);

        assertEquals(DiggingStatus.TRACK_NOT_FOUND, result.status());
        server.verify();
    }

    /** 외부 서버 오류를 곡 없음으로 바꾸지 않고 조회 실패로 전달한다. */
    @Test
    void propagatesMusicBrainzServerError() {
        String recordingId = "00000000-0000-0000-0000-000000000000";

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        server.expect(request -> assertEquals(
                        "/ws/2/recording/" + recordingId,
                        request.getURI().getPath()
                ))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(
                MusicDataLookupException.class,
                () -> new DiggingService(newProvider(builder))
                        .dig(recordingId)
        );
        server.verify();
    }

    /** 테스트용 HTTP 클라이언트를 실제 Provider에 연결한다. */
    private MusicBrainzDataProvider newProvider(
            RestClient.Builder builder
    ) {
        return new MusicBrainzDataProvider(
                new MusicBrainzApiClient(builder.build()),
                new MusicBrainzRecordingParser(),
                new ObjectMapper()
        );
    }
}