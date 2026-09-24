package com.example.musicdigging.digging.model;

import java.util.List;
import java.util.Locale;

/*곡 데이터*/

public record DigTrack(
        String recordingId,
        String title,
        List<TrackCredit> credits,
        List<TrackArtist> artists,
        Integer firstReleaseYear,
        List<String> genres
) {
    public DigTrack {
        if (recordingId == null || recordingId.isBlank()) {
            throw new IllegalArgumentException("곡 ID는 필수입니다.");
        }

        credits = List.copyOf(credits);
        artists = List.copyOf(artists);
        if (firstReleaseYear != null && (firstReleaseYear < 1 || firstReleaseYear > 9999)) {
            throw new IllegalArgumentException("발매연도는 1~9999여야 합니다.");
        }
        genres = genres.stream()
                .map(genre -> genre.strip().toLowerCase(Locale.ROOT))
                .filter(genre -> !genre.isEmpty())
                .distinct()
                .toList();
    }

    // 기존 크레딧 전용 테스트와 호출부도 그대로 사용할 수 있다.
    public DigTrack(String recordingId, String title, List<TrackCredit> credits) {
        this(recordingId, title, credits, List.of(), null, List.of());
    }
}
