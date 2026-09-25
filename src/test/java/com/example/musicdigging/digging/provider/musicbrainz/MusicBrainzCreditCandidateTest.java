/** Work 관계를 통해 같은 크레딧의 Recording 후보를 수집하는지 검증한다. */
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MusicBrainzCreditCandidateTest {

    /** 같은 작곡가의 Work에 실제 연결된 Recording만 후보로 반환한다. */
    @Test
    void findsOnlyRecordingsLinkedToMatchingWork() {
        String worksJson = """
                {
                  "works": [
                    {
                      "id": "work-001",
                      "relations": [{
                        "target-type": "artist",
                        "type-id": "d59d99ea-23d4-4a80-b066-edca32ee158f",
                        "artist": {
                          "id": "person-001",
                          "name": "작곡가 A"
                        },
                        "attributes": []
                      }]
                    }
                  ]
                }
                """;

        String recordingsJson = """
                {
                  "recordings": [
                    {
                      "id": "recording-002",
                      "title": "연결된 후보곡",
                      "artist-credit": [{
                        "artist": {
                          "id": "artist-002",
                          "name": "가수 B"
                        }
                      }],
                      "genres": [{"name": "Rock"}],
                      "relations": [{
                        "target-type": "work",
                        "type-id": "a3005666-a872-32c3-ad06-98af558e99b0",
                        "work": {"id": "work-001"},
                        "attributes": []
                      }]
                    },
                    {
                      "id": "recording-003",
                      "title": "관계가 확인되지 않은 곡",
                      "artist-credit": [],
                      "genres": [],
                      "relations": []
                    }
                  ]
                }
                """;

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server =
                MockRestServiceServer.bindTo(builder).build();

        server.expect(request -> {
                    assertEquals("/ws/2/work", request.getURI().getPath());
                    assertTrue(request.getURI().getQuery()
                            .contains("artist=person-001"));
                })
                .andRespond(withSuccess(
                        worksJson, MediaType.APPLICATION_JSON
                ));

        server.expect(request -> {
                    assertEquals(
                            "/ws/2/recording",
                            request.getURI().getPath()
                    );
                    assertTrue(request.getURI().getQuery()
                            .contains("work=work-001"));
                })
                .andRespond(withSuccess(
                        recordingsJson, MediaType.APPLICATION_JSON
                ));

        MusicBrainzDataProvider provider =
                new MusicBrainzDataProvider(
                        new MusicBrainzApiClient(builder.build()),
                        new MusicBrainzRecordingParser(),
                        new ObjectMapper()
                );

        TrackCredit composer = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        List<DigTrack> candidates =
                provider.findTracksByCredit(composer);

        assertEquals(1, candidates.size());
        assertEquals(
                "recording-002",
                candidates.get(0).recordingId()
        );
        assertEquals("연결된 후보곡", candidates.get(0).title());
        assertEquals(List.of("rock"), candidates.get(0).genres());
        assertTrue(
                candidates.get(0).credits().stream()
                        .anyMatch(composer::hasSamePersonAndRole)
        );
        server.verify();
    }
}