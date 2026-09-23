package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.RecordingWorkLink;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecordingWorkLinkParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MusicBrainzCreditParser parser =
            new MusicBrainzCreditParser();

    @Test
    void readsAllLinkedWorksAndPreservesAttributes() {
        String json = """
                {
                  "id": "recording-001",
                  "relations": [
                    {
                      "target-type": "work",
                      "type-id": "a3005666-a872-32c3-ad06-98af558e99b0",
                      "attributes": ["medley"],
                      "work": {
                        "id": "work-001"
                      }
                    },
                    {
                      "target-type": "artist",
                      "type-id": "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0",
                      "attributes": [],
                      "artist": {
                        "id": "person-001",
                        "name": "프로듀서 A"
                      }
                    },
                    {
                      "target-type": "work",
                      "type-id": "a3005666-a872-32c3-ad06-98af558e99b0",
                      "attributes": ["medley", "partial"],
                      "work": {
                        "id": "work-002"
                      }
                    }
                  ]
                }
                """;

        List<RecordingWorkLink> result =
                parser.parseRecordingWorkLinks(
                        objectMapper.readTree(json)
                );

        assertEquals(
                List.of(
                        new RecordingWorkLink(
                                "MUSICBRAINZ",
                                "recording-001",
                                "work-001",
                                "a3005666-a872-32c3-ad06-98af558e99b0",
                                List.of("medley")
                        ),
                        new RecordingWorkLink(
                                "MUSICBRAINZ",
                                "recording-001",
                                "work-002",
                                "a3005666-a872-32c3-ad06-98af558e99b0",
                                List.of("medley", "partial")
                        )
                ),
                result
        );
    }

    @Test
    void returnsEmptyWhenNoRelationshipsExist() {
        String json = """
                {
                  "id": "recording-001",
                  "relations": []
                }
                """;

        assertTrue(
                parser.parseRecordingWorkLinks(
                        objectMapper.readTree(json)
                ).isEmpty()
        );
    }

    @Test
    void rejectsWorkRelationshipWithoutWorkId() {
        String json = """
                {
                  "id": "recording-001",
                  "relations": [
                    {
                      "target-type": "work",
                      "type-id": "a3005666-a872-32c3-ad06-98af558e99b0",
                      "attributes": [],
                      "work": {}
                    }
                  ]
                }
                """;

        assertThrows(
                MusicDataLookupException.class,
                () -> parser.parseRecordingWorkLinks(
                        objectMapper.readTree(json)
                )
        );
    }
}