// 역할: MusicBrainz의 Artist·Recording·Work를 조회해 디깅용 곡과 후보를 제공한다.
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditEvidence;
import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import com.example.musicdigging.digging.provider.MusicDataProvider;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public class MusicBrainzDataProvider implements MusicDataProvider {

    private static final int WORK_LOOKUP_LIMIT = 5;
    private static final int RECORDING_LOOKUP_LIMIT = 5;
    private static final int PRODUCER_LOOKUP_LIMIT = 3;

    private static final String PRODUCER_TYPE_ID =
            "5c0ceac3-feb4-41f0-868d-dc06f6e27fc0";

    private final MusicBrainzApiClient client;
    private final MusicBrainzRecordingParser parser;
    private final ObjectMapper objectMapper;
    private final MusicBrainzCreditParser creditParser =
            new MusicBrainzCreditParser();
    private final MusicBrainzWorkSelector workSelector =
            new MusicBrainzWorkSelector();

    // API 클라이언트와 JSON 파서를 주입받는다.
    public MusicBrainzDataProvider(
            MusicBrainzApiClient client,
            MusicBrainzRecordingParser parser,
            ObjectMapper objectMapper
    ) {
        this.client = Objects.requireNonNull(client);
        this.parser = Objects.requireNonNull(parser);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    // Recording을 조회하고 연결된 Work의 작곡·작사 크레딧을 합친다.
    @Override
    public Optional<DigTrack> findTrackById(String recordingId) {
        return client.findRecordingJson(recordingId)
                .map(json -> toDigTrackWithWorkCredits(
                        objectMapper.readTree(json)
                ));
    }

    // 역할에 따라 프로듀서 관계 또는 Work 관계로 후보를 찾는다.
    @Override
    public List<DigTrack> findTracksByCredit(TrackCredit credit) {
        Objects.requireNonNull(credit, "조회할 크레딧은 필수입니다.");

        if (credit.role() == CreditRole.PRODUCER) {
            return findProducerTracks(credit);
        }

        if (credit.role() != CreditRole.COMPOSER
                && credit.role() != CreditRole.LYRICIST) {
            throw new UnsupportedOperationException(
                    "지원하지 않는 크레딧 역할입니다: " + credit.role()
            );
        }

        String worksJson = client.browseWorksByArtistJson(
                credit.personId(),
                WORK_LOOKUP_LIMIT
        );

        List<String> matchingWorkIds = workSelector.findMatchingWorkIds(
                objectMapper.readTree(worksJson),
                credit
        );

        Map<String, DigTrack> uniqueTracks = new LinkedHashMap<>();

        for (String workId : matchingWorkIds) {
            String recordingsJson = client.browseRecordingsByWorkJson(
                    workId,
                    RECORDING_LOOKUP_LIMIT
            );

            JsonNode recordings = objectMapper.readTree(recordingsJson)
                    .path("recordings");

            if (!recordings.isArray()) {
                throw new MusicDataLookupException(
                        "Recording 목록 응답에 recordings 배열이 없습니다."
                );
            }

            for (JsonNode recording : recordings) {
                if (!isLinkedToWork(recording, workId)) {
                    continue;
                }

                DigTrack candidate = toCandidateTrack(recording, credit);
                uniqueTracks.putIfAbsent(
                        candidate.recordingId(),
                        candidate
                );

                // 한 Work에서는 하나만 선택한다.
                break;
            }
        }

        return List.copyOf(uniqueTracks.values());
    }

    // Artist의 producer 관계에서 후보 ID를 찾고 Recording 상세 관계로 재검증한다.
    private List<DigTrack> findProducerTracks(TrackCredit credit) {
        String artistJson = client.findArtistJson(credit.personId())
                .orElseThrow(() -> new MusicDataLookupException(
                        "프로듀서 Artist를 찾을 수 없습니다: "
                                + credit.personId()
                ));

        JsonNode relations = objectMapper.readTree(artistJson)
                .path("relations");

        if (!relations.isArray()) {
            throw new MusicDataLookupException(
                    "Artist 응답에 relations 배열이 없습니다."
            );
        }

        Set<String> recordingIds = new LinkedHashSet<>();

        for (JsonNode relation : relations) {
            if (!"recording".equals(
                    relation.path("target-type").asText()
            )) {
                continue;
            }

            if (!PRODUCER_TYPE_ID.equals(
                    relation.path("type-id").asText()
            )) {
                continue;
            }

            JsonNode id = relation.path("recording").path("id");
            if (!id.isTextual() || id.asText().isBlank()) {
                throw new MusicDataLookupException(
                        "Producer 관계에 Recording ID가 없습니다."
                );
            }

            recordingIds.add(id.asText());
        }

        Map<String, DigTrack> uniqueTracks = new LinkedHashMap<>();
        int lookedUp = 0;

        for (String recordingId : recordingIds) {
            if (lookedUp >= PRODUCER_LOOKUP_LIMIT) {
                break;
            }

            lookedUp++;

            Optional<String> recordingJson =
                    client.findRecordingJson(recordingId);

            if (recordingJson.isEmpty()) {
                continue;
            }

            JsonNode recording =
                    objectMapper.readTree(recordingJson.get());

            boolean verified = creditParser
                    .parseRecordingProducers(recording)
                    .stream()
                    .map(CreditEvidence::credit)
                    .anyMatch(found ->
                            found.hasSamePersonAndRole(credit)
                    );

            if (!verified) {
                continue;
            }

            DigTrack candidate =
                    toCandidateTrack(recording, credit);

            uniqueTracks.putIfAbsent(
                    candidate.recordingId(),
                    candidate
            );
        }

        return List.copyOf(uniqueTracks.values());
    }

    // 아티스트의 앞·중간·뒤 Recording 구간에서 후보를 수집한다.
    @Override
    public List<DigTrack> findTracksByArtist(
            String artistId,
            int limit
    ) {
        if (artistId == null || artistId.isBlank()) {
            throw new IllegalArgumentException(
                    "아티스트 ID는 필수입니다."
            );
        }

        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "조회 limit는 1~100이어야 합니다."
            );
        }

        int pageSize = limit;

        JsonNode firstPage = objectMapper.readTree(
                client.browseRecordingsByArtistJson(
                        artistId,
                        pageSize
                )
        );

        JsonNode firstRecordings = requireRecordings(firstPage);
        int totalCount = readRecordingCount(
                firstPage,
                firstRecordings.size()
        );

        Map<String, DigTrack> uniqueByTitle =
                new LinkedHashMap<>();

        addMatchingArtistTracks(
                firstRecordings,
                artistId,
                uniqueByTitle
        );

        int lastOffset = Math.max(0, totalCount - pageSize);

        Set<Integer> extraOffsets = new LinkedHashSet<>();
        if (lastOffset > 0) {
            extraOffsets.add(lastOffset / 2);
            extraOffsets.add(lastOffset);
        }

        extraOffsets.remove(0);

        for (int offset : extraOffsets) {
            if (uniqueByTitle.size() >= limit) {
                break;
            }

            JsonNode page = objectMapper.readTree(
                    client.browseRecordingsByArtistJson(
                            artistId,
                            pageSize,
                            offset
                    )
            );

            addMatchingArtistTracks(
                    requireRecordings(page),
                    artistId,
                    uniqueByTitle
            );
        }

        return uniqueByTitle.values()
                .stream()
                .limit(limit)
                .toList();
    }

    // 장르와 발매연도 기준 후보 조회는 다음 단계에서 구현한다.
    @Override
    public List<DigTrack> findTracksByGenreAndYearRange(
            String genre,
            int fromYear,
            int toYear,
            int limit
    ) {
        throw new UnsupportedOperationException(
                "장르·연도 기준 후보 조회는 아직 지원하지 않습니다."
        );
    }

    // Recording 목록 필드가 배열인지 검사한다.
    private JsonNode requireRecordings(JsonNode response) {
        JsonNode recordings = response.path("recordings");

        if (!recordings.isArray()) {
            throw new MusicDataLookupException(
                    "아티스트 Recording 응답에 recordings 배열이 없습니다."
            );
        }

        return recordings;
    }

    // browse 응답의 전체 Recording 수를 읽는다.
    private int readRecordingCount(
            JsonNode response,
            int firstPageSize
    ) {
        JsonNode count = response.path("recording-count");

        // 건수 필드가 없는 기존 오프라인 테스트는 첫 페이지로 끝낸다.
        if (count.isMissingNode()) {
            return firstPageSize;
        }

        if (!count.isIntegralNumber() || count.asInt() < 0) {
            throw new MusicDataLookupException(
                    "recording-count 값이 올바르지 않습니다."
            );
        }

        return Math.max(count.asInt(), firstPageSize);
    }

    // 요청한 아티스트가 참여한 곡만 제목 기준으로 하나씩 담는다.
    private void addMatchingArtistTracks(
            JsonNode recordings,
            String artistId,
            Map<String, DigTrack> uniqueByTitle
    ) {
        for (JsonNode recording : recordings) {
            DigTrack track = parser.parse(recording);

            boolean matchesArtist = track.artists()
                    .stream()
                    .anyMatch(artist ->
                            artistId.equals(artist.artistId())
                    );

            if (!matchesArtist) {
                continue;
            }

            String titleKey = track.title()
                    .strip()
                    .toLowerCase(Locale.ROOT);

            uniqueByTitle.putIfAbsent(titleKey, track);
        }
    }

    // 시작 Recording의 Work를 조회해 작곡·작사 크레딧을 중복 없이 합친다.
    private DigTrack toDigTrackWithWorkCredits(JsonNode recording) {
        DigTrack basicTrack = parser.parse(recording);
        List<TrackCredit> credits =
                new ArrayList<>(basicTrack.credits());

        Set<String> workIds = new LinkedHashSet<>();

        for (var link : creditParser.parseRecordingWorkLinks(recording)) {
            workIds.add(link.workId());
        }

        for (String workId : workIds) {
            Optional<String> workJson = client.findWorkJson(workId);

            if (workJson.isEmpty()) {
                continue;
            }

            JsonNode work = objectMapper.readTree(workJson.get());

            for (CreditEvidence evidence
                    : creditParser.parseWorkCredits(work)) {
                addCreditIfMissing(credits, evidence.credit());
            }
        }

        return new DigTrack(
                basicTrack.recordingId(),
                basicTrack.title(),
                credits,
                basicTrack.artists(),
                basicTrack.firstReleaseYear(),
                basicTrack.genres()
        );
    }

    // Recording의 performance 관계가 요청한 Work를 가리키는지 확인한다.
    private boolean isLinkedToWork(
            JsonNode recording,
            String workId
    ) {
        return creditParser.parseRecordingWorkLinks(recording)
                .stream()
                .anyMatch(link -> workId.equals(link.workId()));
    }

    // 확인된 크레딧을 후보 Recording에 중복 없이 붙인다.
    private DigTrack toCandidateTrack(
            JsonNode recording,
            TrackCredit credit
    ) {
        DigTrack basicTrack = parser.parse(recording);
        List<TrackCredit> credits =
                new ArrayList<>(basicTrack.credits());

        addCreditIfMissing(credits, credit);

        return new DigTrack(
                basicTrack.recordingId(),
                basicTrack.title(),
                credits,
                basicTrack.artists(),
                basicTrack.firstReleaseYear(),
                basicTrack.genres()
        );
    }

    // 같은 사람과 역할의 크레딧이 없을 때만 추가한다.
    private void addCreditIfMissing(
            List<TrackCredit> credits,
            TrackCredit credit
    ) {
        boolean exists = credits.stream()
                .anyMatch(saved ->
                        saved.hasSamePersonAndRole(credit)
                );

        if (!exists) {
            credits.add(credit);
        }
    }
}