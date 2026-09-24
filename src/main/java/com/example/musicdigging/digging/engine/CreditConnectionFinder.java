package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;

import java.util.ArrayList;
import java.util.List;

public class CreditConnectionFinder {
    /*두 곡 사이에 같은 크레딧 연결점이 있는가?*/

    public List<TrackCredit> findConnections(
            DigTrack startTrack,
            DigTrack candidateTrack
    ) {
        if (startTrack.recordingId().equals(candidateTrack.recordingId())) {
            return List.of();
        }

        List<TrackCredit> connections = new ArrayList<>();

        for (TrackCredit startCredit : startTrack.credits()) {
            for (TrackCredit candidateCredit : candidateTrack.credits()) {
                if (startCredit.hasSamePersonAndRole(candidateCredit)) {
                    connections.add(startCredit);
                    break;
                }
            }
        }

        return connections;
    }
}