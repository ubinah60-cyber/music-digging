/** Work 목록에서 지정한 참여자와 역할이 일치하는 Work ID를 고른다. */
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditEvidence;
import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MusicBrainzWorkSelector {

    private final MusicBrainzCreditParser creditParser =
            new MusicBrainzCreditParser();

    /** Work 목록에서 같은 참여자 ID와 역할이 확인된 Work ID만 반환한다. */
    public List<String> findMatchingWorkIds(
            JsonNode browseResponse,
            TrackCredit targetCredit
    ) {
        Objects.requireNonNull(targetCredit, "조회할 크레딧은 필수입니다.");

        if (targetCredit.role() != CreditRole.COMPOSER
                && targetCredit.role() != CreditRole.LYRICIST) {
            throw new IllegalArgumentException(
                    "Work 조회는 작곡가·작사가 역할만 지원합니다."
            );
        }

        if (browseResponse == null || !browseResponse.isObject()) {
            throw new MusicDataLookupException(
                    "Work 목록 응답은 JSON 객체여야 합니다."
            );
        }

        JsonNode works = browseResponse.path("works");
        if (!works.isArray()) {
            throw new MusicDataLookupException(
                    "Work 목록 응답에 works 배열이 없습니다."
            );
        }

        List<String> matchedIds = new ArrayList<>();

        for (JsonNode work : works) {
            boolean matched = creditParser.parseWorkCredits(work)
                    .stream()
                    .map(CreditEvidence::credit)
                    .anyMatch(targetCredit::hasSamePersonAndRole);

            if (matched) {
                matchedIds.add(work.path("id").asText());
            }
        }

        return matchedIds.stream().distinct().toList();
    }
}