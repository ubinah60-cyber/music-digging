// 역할: MusicBrainz API에 요청하고 Artist·Recording·Work 조회 결과를 JSON으로 제공한다.
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
public class MusicBrainzApiClient {

    private static final String BASE_URL =
            "https://musicbrainz.org/ws/2";

    private static final String USER_AGENT =
            "Digger/0.1 (https://github.com/ubinah60-cyber/music-digging)";

    private static final long REQUEST_INTERVAL_NANOS =
            TimeUnit.MILLISECONDS.toNanos(1100);

    private final RestClient restClient;
    private long nextRequestAtNanos;

    // HTTP 요청에 사용할 RestClient를 주입받는다.
    public MusicBrainzApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    // Recording ID로 상세 정보를 조회하며, 404면 빈 값을 반환한다.
    public Optional<String> findRecordingJson(String recordingId) {
        validateId(recordingId);

        String url = BASE_URL
                + "/recording/" + recordingId
                + "?inc=artist-credits+artist-rels+work-rels+genres&fmt=json";

        return findJson(url);
    }

    // Work ID로 작곡·작사 관계를 조회하며, 404면 빈 값을 반환한다.
    public Optional<String> findWorkJson(String workId) {
        validateId(workId);

        String url = BASE_URL
                + "/work/" + workId
                + "?inc=artist-rels&fmt=json";

        return findJson(url);
    }

    // Artist ID로 해당 아티스트와 Recording의 관계를 조회한다.
    public Optional<String> findArtistJson(String artistId) {
        validateId(artistId);

        String url = BASE_URL
                + "/artist/" + artistId
                + "?inc=recording-rels&fmt=json";

        return findJson(url);
    }

    // 참여자 ID에 연결된 Work 목록을 조회한다.
    public String browseWorksByArtistJson(String artistId, int limit) {
        validateId(artistId);
        validateLimit(limit);

        String url = BASE_URL
                + "/work?artist=" + artistId
                + "&limit=" + limit
                + "&inc=artist-rels&fmt=json";

        return requestJson(url);
    }

    // Work ID에 연결된 Recording 목록을 조회한다.
    public String browseRecordingsByWorkJson(String workId, int limit) {
        validateId(workId);
        validateLimit(limit);

        String url = BASE_URL
                + "/recording?work=" + workId
                + "&limit=" + limit
                + "&inc=artist-credits+artist-rels+work-rels+genres&fmt=json";

        return requestJson(url);
    }

    // 아티스트 Recording 목록의 첫 페이지를 조회한다.
    public String browseRecordingsByArtistJson(
            String artistId,
            int limit
    ) {
        return browseRecordingsByArtistJson(artistId, limit, 0);
    }

    // 아티스트 Recording 목록에서 offset 위치부터 조회한다.
    public String browseRecordingsByArtistJson(
            String artistId,
            int limit,
            int offset
    ) {
        validateId(artistId);
        validateLimit(limit);
        validateOffset(offset);

        String url = BASE_URL
                + "/recording?artist=" + artistId
                + "&limit=" + limit
                + "&offset=" + offset
                + "&inc=artist-credits+artist-rels+work-rels+genres&fmt=json";

        return requestJson(url);
    }

    // 단일 항목 조회에서 404를 '데이터 없음'으로 변환한다.
    private Optional<String> findJson(String url) {
        try {
            return Optional.of(requestJson(url));
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }

    // 요청 간격을 지키고 MusicBrainz의 잘못된 MBID 오류를 입력 오류로 분류한다.
    private synchronized String requestJson(String url) {
        waitForRequestSlot();

        try {
            String json = restClient.get()
                    .uri(url)
                    .header("User-Agent", USER_AGENT)
                    .retrieve()
                    .body(String.class);

            if (json == null || json.isBlank()) {
                throw new MusicDataLookupException(
                        "MusicBrainz 응답 본문이 비어 있습니다."
                );
            }

            return json;
        } catch (HttpClientErrorException.NotFound exception) {
            throw exception;
        } catch (HttpClientErrorException.BadRequest exception) {
            if (exception.getResponseBodyAsString()
                    .contains("Invalid mbid.")) {
                throw new IllegalArgumentException(
                        "올바른 MusicBrainz ID가 아닙니다.",
                        exception
                );
            }

            throw new MusicDataLookupException(
                    "MusicBrainz 요청에 실패했습니다.",
                    exception
            );
        } catch (RestClientException exception) {
            throw new MusicDataLookupException(
                    "MusicBrainz 요청에 실패했습니다.",
                    exception
            );
        }
    }

    // ID 입력이 비어 있는지 검사한다.
    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                    "MusicBrainz ID는 필수입니다."
            );
        }
    }

    // MusicBrainz 목록 조회 제한값을 검사한다.
    private void validateLimit(int limit) {
        if (limit < 1 || limit > 100) {
            throw new IllegalArgumentException(
                    "조회 limit는 1~100이어야 합니다."
            );
        }
    }

    // 페이지 시작 위치가 음수인지 검사한다.
    private void validateOffset(int offset) {
        if (offset < 0) {
            throw new IllegalArgumentException(
                    "조회 offset은 0 이상이어야 합니다."
            );
        }
    }

    // 직전 요청 이후 최소 1.1초가 지나도록 기다린다.
    private void waitForRequestSlot() {
        long remainingNanos =
                nextRequestAtNanos - System.nanoTime();

        if (remainingNanos > 0) {
            try {
                TimeUnit.NANOSECONDS.sleep(remainingNanos);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                throw new MusicDataLookupException(
                        "MusicBrainz 요청 대기 중 중단되었습니다.",
                        exception
                );
            }
        }

        nextRequestAtNanos =
                System.nanoTime() + REQUEST_INTERVAL_NANOS;
    }
}