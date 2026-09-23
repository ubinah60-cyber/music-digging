package com.example.musicdigging.digging.provider;

import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;

import java.util.List;
import java.util.Optional;

public interface MusicDataProvider {

    Optional<DigTrack> findTrackById(String recordingId);

    List<DigTrack> findTracksByCredit(TrackCredit credit);
}