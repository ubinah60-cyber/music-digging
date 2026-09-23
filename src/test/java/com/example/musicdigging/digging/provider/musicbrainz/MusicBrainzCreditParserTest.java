package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditEvidence;
import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MusicBrainzCreditParserTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final MusicBrainzCreditParser parser =
            new MusicBrainzCreditParser();

    @Test
    void readsProducerAndPreservesEvidence() {
        String json = """
                {
                  "id": "recording-001",
                  "relations": [
                    {
                      "target-type": "artist",
                      "type": "producer",
                      "type-id": "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0",
                      "attributes": ["co"],
                      "artist": {
                        "id": "person-001",
                        "name": "프로듀서 A"
                      }
                    },
                    {
                      "target-type": "artist",
                      "type": "unrelated-test-role",
                      "type-id": "unrelated-test-type",
                      "attributes": [],
                      "artist": {
                        "id": "person-002",
                        "name": "다른 참여자"
                      }
                    }
                  ]
                }
                """;

        List<CreditEvidence> result =
                parser.parseRecordingProducers(objectMapper.readTree(json));

        assertEquals(1, result.size());

        CreditEvidence evidence = result.get(0);

        assertEquals("person-001", evidence.credit().personId());
        assertEquals("프로듀서 A", evidence.credit().personName());
        assertEquals(CreditRole.PRODUCER, evidence.credit().role());
        assertEquals("MUSICBRAINZ", evidence.provider());
        assertEquals("recording", evidence.sourceEntityType());
        assertEquals("recording-001", evidence.sourceEntityId());
        assertEquals(
                "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0",
                evidence.relationshipTypeId()
        );
        assertEquals(List.of("co"), evidence.attributes());
    }

    @Test
    void returnsEmptyForEmptyRelations() {
        String json = """
                {
                  "id": "recording-001",
                  "relations": []
                }
                """;

        assertTrue(
                parser.parseRecordingProducers(
                        objectMapper.readTree(json)
                ).isEmpty()
        );
    }

    @Test
    void rejectsMissingRelationsInsteadOfTreatingThemAsNoCredits() {
        String json = """
                {
                  "id": "recording-001"
                }
                """;

        assertThrows(
                MusicDataLookupException.class,
                () -> parser.parseRecordingProducers(
                        objectMapper.readTree(json)
                )
        );
    }

    @Test
    void rejectsProducerWithoutPersonId() {
        String json = """
                {
                  "id": "recording-001",
                  "relations": [
                    {
                      "target-type": "artist",
                      "type": "producer",
                      "type-id": "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0",
                      "attributes": [],
                      "artist": {
                        "name": "프로듀서 A"
                      }
                    }
                  ]
                }
                """;

        assertThrows(
                MusicDataLookupException.class,
                () -> parser.parseRecordingProducers(
                        objectMapper.readTree(json)
                )
        );
    }

    @Test
    void readsComposerAndLyricistAsSeparateRolesForSamePerson() {
        String json = """
            {
              "id": "work-001",
              "relations": [
                {
                  "target-type": "artist",
                  "type": "composer",
                  "type-id": "d59d99ea-23d4-4a80-b066-edca32ee158f",
                  "attributes": ["additional"],
                  "artist": {
                    "id": "person-001",
                    "name": "참여자 A"
                  }
                },
                {
                  "target-type": "artist",
                  "type": "lyricist",
                  "type-id": "3e48faba-ec01-47fd-8e89-30e81161661c",
                  "attributes": [],
                  "artist": {
                    "id": "person-001",
                    "name": "참여자 A"
                  }
                }
              ]
            }
            """;

        List<CreditEvidence> result =
                parser.parseWorkCredits(objectMapper.readTree(json));

        assertEquals(2, result.size());

        CreditEvidence composer = result.get(0);
        CreditEvidence lyricist = result.get(1);

        assertEquals("person-001", composer.credit().personId());
        assertEquals("person-001", lyricist.credit().personId());

        assertEquals(CreditRole.COMPOSER, composer.credit().role());
        assertEquals(CreditRole.LYRICIST, lyricist.credit().role());

        assertEquals(
                "d59d99ea-23d4-4a80-b066-edca32ee158f",
                composer.relationshipTypeId()
        );
        assertEquals(
                "3e48faba-ec01-47fd-8e89-30e81161661c",
                lyricist.relationshipTypeId()
        );

        for (CreditEvidence evidence : result) {
            assertEquals("MUSICBRAINZ", evidence.provider());
            assertEquals("work", evidence.sourceEntityType());
            assertEquals("work-001", evidence.sourceEntityId());
        }

        assertEquals(List.of("additional"), composer.attributes());
        assertTrue(lyricist.attributes().isEmpty());
    }

    @Test
    void doesNotGuessComposerOrLyricistFromWriterRelationship() {
        String json = """
            {
              "id": "work-001",
              "relations": [
                {
                  "target-type": "artist",
                  "type": "writer",
                  "type-id": "a255bca1-b157-4518-9108-7b147dc3fc68",
                  "attributes": [],
                  "artist": {
                    "id": "person-001",
                    "name": "참여자 A"
                  }
                }
              ]
            }
            """;

        List<CreditEvidence> result =
                parser.parseWorkCredits(objectMapper.readTree(json));

        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyForWorkWithoutRelationships() {
        String json = """
            {
              "id": "work-001",
              "relations": []
            }
            """;

        assertTrue(
                parser.parseWorkCredits(
                        objectMapper.readTree(json)
                ).isEmpty()
        );
    }

    @Test
    void rejectsWorkWhenRelationshipsWereNotIncluded() {
        String json = """
            {
              "id": "work-001"
            }
            """;

        assertThrows(
                MusicDataLookupException.class,
                () -> parser.parseWorkCredits(
                        objectMapper.readTree(json)
                )
        );
    }
}