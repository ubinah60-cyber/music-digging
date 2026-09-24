package com.example.musicdigging.digging.provider;

import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;

import java.util.List;
import java.util.Optional;

public interface MusicDataProvider {

    Optional<DigTrack> findTrackById(String recordingId);

    List<DigTrack> findTracksByCredit(TrackCredit credit);

    // limit는 조회 결과 상한이다. 시작곡이 포함되면 엔진에서 제외한다.
    List<DigTrack> findTracksByArtist(String artistId, int limit);

    // 연도 양 끝을 포함하며, 장르는 DigTrack과 동일하게 정규화한다.
    List<DigTrack> findTracksByGenreAndYearRange(
            String genre, int fromYear, int toYear, int limit);
}
