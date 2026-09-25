package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditEvidence;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import tools.jackson.databind.JsonNode;
import com.example.musicdigging.digging.model.TrackArtist;
import java.util.ArrayList;

import java.util.List;

/** Recording JSON을 디깅 엔진에서 사용하는 DigTrack으로 변환한다. */

public class MusicBrainzRecordingParser {

    private final MusicBrainzCreditParser creditParser =
            new MusicBrainzCreditParser();
    /** 곡 ID·제목, 직접 연결된 프로듀서, 가수와 장르를 읽는다. */
    public DigTrack parse(JsonNode recording) {
        if (recording == null || !recording.isObject()) {
            throw new MusicDataLookupException("Recording 응답은 JSON 객체여야 합니다.");
        }

        String id = requiredText(recording, "id");
        String title = requiredText(recording, "title");

        List<TrackCredit> producers = creditParser
                .parseRecordingProducers(recording)
                .stream()
                .map(CreditEvidence::credit)
                .distinct()
                .toList();

        return new DigTrack(
                id,
                title,
                producers,
                parseArtists(recording),
                null,
                parseGenres(recording)
        );
    }
    /** 필수 문자열 필드를 읽고, 없거나 비어 있으면 조회 오류로 처리한다. */

    /** 아티스트 표기에서 실제 artist 객체만 골라 곡의 가수로 변환한다. */
    private List<TrackArtist> parseArtists(JsonNode recording) {
        List<TrackArtist> artists = new ArrayList<>();
        JsonNode artistCredit = recording.path("artist-credit");

        if (artistCredit.isArray()) {
            for (JsonNode entry : artistCredit) {
                JsonNode artist = entry.path("artist");
                if (artist.isObject()) {
                    artists.add(new TrackArtist(
                            requiredText(artist, "id"),
                            requiredText(artist, "name")
                    ));
                }
            }
        }

        return artists.stream().distinct().toList();
    }

    /** MusicBrainz의 장르 이름을 읽는다. 소문자 정규화는 DigTrack이 담당한다. */
    private List<String> parseGenres(JsonNode recording) {
        List<String> genres = new ArrayList<>();
        JsonNode genreNodes = recording.path("genres");

        if (genreNodes.isArray()) {
            for (JsonNode genre : genreNodes) {
                JsonNode name = genre.path("name");
                if (name.isTextual() && !name.asText().isBlank()) {
                    genres.add(name.asText());
                }
            }
        }

        return genres;
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