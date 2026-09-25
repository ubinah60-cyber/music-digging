/** Work 후보가 이름이 아닌 참여자 ID와 역할로 걸러지는지 검증한다. */
package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.model.CreditRole;
import com.example.musicdigging.digging.model.TrackCredit;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MusicBrainzWorkSelectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MusicBrainzWorkSelector selector =
            new MusicBrainzWorkSelector();

    /** 같은 ID의 작곡가만 고르고 이름 차이·다른 역할은 구분한다. */
    @Test
    void selectsWorksByPersonIdAndRole() {
        String json = """
                {
                  "works": [
                    {
                      "id": "work-001",
                      "relations": [{
                        "target-type": "artist",
                        "type-id": "d59d99ea-23d4-4a80-b066-edca32ee158f",
                        "artist": {
                          "id": "person-001",
                          "name": "다른 이름 표기"
                        },
                        "attributes": []
                      }]
                    },
                    {
                      "id": "work-002",
                      "relations": [{
                        "target-type": "artist",
                        "type-id": "3e48faba-ec01-47fd-8e89-30e81161661c",
                        "artist": {
                          "id": "person-001",
                          "name": "작사가 A"
                        },
                        "attributes": []
                      }]
                    },
                    {
                      "id": "work-003",
                      "relations": [{
                        "target-type": "artist",
                        "type-id": "d59d99ea-23d4-4a80-b066-edca32ee158f",
                        "artist": {
                          "id": "person-002",
                          "name": "다른 작곡가"
                        },
                        "attributes": []
                      }]
                    }
                  ]
                }
                """;

        TrackCredit composer = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        assertEquals(
                List.of("work-001"),
                selector.findMatchingWorkIds(
                        objectMapper.readTree(json),
                        composer
                )
        );
    }

    /** works 배열 누락을 후보 없음으로 취급하지 않는다. */
    @Test
    void rejectsMissingWorksArray() {
        TrackCredit composer = new TrackCredit(
                "person-001", "작곡가 A", CreditRole.COMPOSER
        );

        assertThrows(
                MusicDataLookupException.class,
                () -> selector.findMatchingWorkIds(
                        objectMapper.readTree("{}"),
                        composer
                )
        );
    }
}