package com.example.musicdigging.digging.model;

import java.util.Objects;

/** value: 크레딧은 역할:참여자ID, 가수는 ID, 장르는 정규화된 장르명. */
public record CandidateReason(
        CandidateReasonType type,
        String value,
        Integer fromYear,
        Integer toYear
) {
    public CandidateReason {
        Objects.requireNonNull(type, "수집 경로는 필수입니다.");
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("수집 근거 값은 필수입니다.");
        }
        if (type == CandidateReasonType.GENRE_AND_YEAR) {
            if (fromYear == null || toYear == null || fromYear < 1
                    || toYear > 9999 || fromYear > toYear) {
                throw new IllegalArgumentException("유효한 연도 범위가 필요합니다.");
            }
        } else if (fromYear != null || toYear != null) {
            throw new IllegalArgumentException("연도 범위는 장르+연도 경로에서만 사용합니다.");
        }
    }

    public static CandidateReason credit(TrackCredit credit) {
        return new CandidateReason(CandidateReasonType.CREDIT,
                credit.role() + ":" + credit.personId(), null, null);
    }

    public static CandidateReason artist(String artistId) {
        return new CandidateReason(CandidateReasonType.SAME_ARTIST, artistId, null, null);
    }

    public static CandidateReason genreAndYear(String genre, int fromYear, int toYear) {
        return new CandidateReason(CandidateReasonType.GENRE_AND_YEAR, genre, fromYear, toYear);
    }
}
