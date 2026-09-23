package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditEvidence;
import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import tools.jackson.databind.JsonNode;
import com.example.musicdigging.digging.model.RecordingWorkLink;

import java.util.ArrayList;
import java.util.List;

public class MusicBrainzCreditParser {

    private static final String PRODUCER_TYPE_ID =
            "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0";

    private static final String COMPOSER_TYPE_ID =
            "d59d99ea-23d4-4a80-b066-edca32ee158f";

    private static final String LYRICIST_TYPE_ID =
            "3e48faba-ec01-47fd-8e89-30e81161661c";

    private static final String PERFORMANCE_TYPE_ID =
            "a3005666-a872-32c3-ad06-98af558e99b0";

    public List<CreditEvidence> parseRecordingProducers(
            JsonNode recording
    ) {
        if (recording == null || !recording.isObject()) {
            throw new MusicDataLookupException(
                    "Recording 응답은 JSON 객체여야 합니다."
            );
        }

        String recordingId = requiredText(recording, "id");
        JsonNode relations = recording.path("relations");

        if (!relations.isArray()) {
            throw new MusicDataLookupException(
                    "Recording 응답에 relations 배열이 없습니다."
            );
        }

        List<CreditEvidence> result = new ArrayList<>();

        for (JsonNode relation : relations) {
            String targetType = requiredText(relation, "target-type");
            String typeId = requiredText(relation, "type-id");

            if (!"artist".equals(targetType)
                    || !PRODUCER_TYPE_ID.equals(typeId)) {
                continue;
            }

            JsonNode artist = relation.path("artist");

            TrackCredit credit = new TrackCredit(
                    requiredText(artist, "id"),
                    requiredText(artist, "name"),
                    CreditRole.PRODUCER
            );

            List<String> attributes = new ArrayList<>();
            JsonNode attributeNodes = relation.path("attributes");

            if (!attributeNodes.isArray()) {
                throw new MusicDataLookupException(
                        "Producer 관계에 attributes 배열이 없습니다."
                );
            }

            for (JsonNode attribute : attributeNodes) {
                if (!attribute.isTextual()) {
                    throw new MusicDataLookupException(
                            "관계 속성은 문자열이어야 합니다."
                    );
                }

                attributes.add(attribute.asText());
            }

            result.add(new CreditEvidence(
                    credit,
                    "MUSICBRAINZ",
                    "recording",
                    recordingId,
                    typeId,
                    attributes
            ));
        }

        return List.copyOf(result);
    }

    public List<CreditEvidence> parseWorkCredits(JsonNode work) {
        if (work == null || !work.isObject()) {
            throw new MusicDataLookupException(
                    "Work 응답은 JSON 객체여야 합니다."
            );
        }

        String workId = requiredText(work, "id");
        JsonNode relations = work.path("relations");

        if (!relations.isArray()) {
            throw new MusicDataLookupException(
                    "Work 응답에 relations 배열이 없습니다."
            );
        }

        List<CreditEvidence> result = new ArrayList<>();

        for (JsonNode relation : relations) {
            String targetType = requiredText(relation, "target-type");
            String typeId = requiredText(relation, "type-id");

            if (!"artist".equals(targetType)) {
                continue;
            }

            CreditRole role;

            if (COMPOSER_TYPE_ID.equals(typeId)) {
                role = CreditRole.COMPOSER;
            } else if (LYRICIST_TYPE_ID.equals(typeId)) {
                role = CreditRole.LYRICIST;
            } else {
                continue;
            }

            JsonNode artist = relation.path("artist");

            TrackCredit credit = new TrackCredit(
                    requiredText(artist, "id"),
                    requiredText(artist, "name"),
                    role
            );

            List<String> attributes = new ArrayList<>();
            JsonNode attributeNodes = relation.path("attributes");

            if (!attributeNodes.isArray()) {
                throw new MusicDataLookupException(
                        "Work 참여 관계에 attributes 배열이 없습니다."
                );
            }

            for (JsonNode attribute : attributeNodes) {
                if (!attribute.isTextual()) {
                    throw new MusicDataLookupException(
                            "관계 속성은 문자열이어야 합니다."
                    );
                }

                attributes.add(attribute.asText());
            }

            result.add(new CreditEvidence(
                    credit,
                    "MUSICBRAINZ",
                    "work",
                    workId,
                    typeId,
                    attributes
            ));
        }

        return List.copyOf(result);
    }

    public List<RecordingWorkLink> parseRecordingWorkLinks(
            JsonNode recording
    ) {
        if (recording == null || !recording.isObject()) {
            throw new MusicDataLookupException(
                    "Recording 응답은 JSON 객체여야 합니다."
            );
        }

        String recordingId = requiredText(recording, "id");
        JsonNode relations = recording.path("relations");

        if (!relations.isArray()) {
            throw new MusicDataLookupException(
                    "Recording 응답에 relations 배열이 없습니다."
            );
        }

        List<RecordingWorkLink> result = new ArrayList<>();

        for (JsonNode relation : relations) {
            String targetType = requiredText(relation, "target-type");
            String typeId = requiredText(relation, "type-id");

            if (!"work".equals(targetType)
                    || !PERFORMANCE_TYPE_ID.equals(typeId)) {
                continue;
            }

            String workId = requiredText(relation.path("work"), "id");

            List<String> attributes = new ArrayList<>();
            JsonNode attributeNodes = relation.path("attributes");

            if (!attributeNodes.isArray()) {
                throw new MusicDataLookupException(
                        "Performance 관계에 attributes 배열이 없습니다."
                );
            }

            for (JsonNode attribute : attributeNodes) {
                if (!attribute.isTextual()) {
                    throw new MusicDataLookupException(
                            "관계 속성은 문자열이어야 합니다."
                    );
                }

                attributes.add(attribute.asText());
            }

            result.add(new RecordingWorkLink(
                    "MUSICBRAINZ",
                    recordingId,
                    workId,
                    typeId,
                    attributes
            ));
        }

        return List.copyOf(result);
    }

    private String requiredText(JsonNode node, String field) {
        JsonNode value = node.path(field);

        if (!value.isTextual() || value.asText().isBlank()) {
            throw new MusicDataLookupException(
                    "필수 문자열 필드가 없거나 잘못되었습니다: " + field
            );
        }

        return value.asText();
    }
}