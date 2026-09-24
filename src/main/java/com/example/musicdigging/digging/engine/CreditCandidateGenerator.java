 package com.example.musicdigging.digging.engine;

import com.example.musicdigging.digging.model.DigCandidate;
import com.example.musicdigging.digging.model.DigTrack;
import com.example.musicdigging.digging.model.TrackCredit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreditCandidateGenerator {

    // 두 곡 사이에 같은 사람 + 같은 역할의 크레딧이 있는지 찾아주는 객체
    // final이므로 한 번 생성된 connectionFinder 참조는 다른 객체로 변경할 수 없음
    private final CreditConnectionFinder connectionFinder =
            new CreditConnectionFinder();

    /**
     * 시작곡과 후보곡 목록을 받아
     * 실제로 크레딧 연결점이 있는 곡들만 DigCandidate로 만들어 반환
     */
    public List<DigCandidate> generate(
            DigTrack startTrack,              // 현재 디깅을 시작한 곡
            List<DigTrack> candidateTracks    // 연결 가능성을 검사할 후보곡 목록
    ) {

        // 최종 후보곡들을 recordingId 기준으로 저장하는 Map
        // Key   : recordingId
        // Value : DigCandidate
        //
        // 같은 곡이 여러 번 발견되더라도 recordingId 기준으로 하나로 합치기 위해 사용
        // LinkedHashMap이므로 후보가 들어온 순서도 유지됨
        Map<String, DigCandidate> candidatesById = new LinkedHashMap<>();

        // 후보곡 목록을 하나씩 순회
        for (DigTrack track : candidateTracks) {

            // 시작곡과 현재 후보곡 사이의 공통 크레딧을 찾음
            // 예: 같은 작곡가, 같은 프로듀서 등
            List<TrackCredit> connections =
                    connectionFinder.findConnections(startTrack, track);

            // 시작곡과 연결되는 크레딧이 하나도 없다면
            // 이 곡은 디깅 후보가 아니므로 다음 곡으로 넘어감
            if (connections.isEmpty()) {
                continue;
            }

            // 현재 후보곡이 이미 candidatesById에 저장되어 있는지 확인
            // 있으면 기존 DigCandidate 반환
            // 없으면 null 반환
            DigCandidate existing =
                    candidatesById.get(track.recordingId());

            // 기존 연결정보와 새 연결정보를 합쳐서 저장할 임시 리스트
            List<TrackCredit> mergedConnections = new ArrayList<>();

            // 이미 같은 후보곡이 등록되어 있었다면
            // 기존에 저장되어 있던 연결정보를 먼저 복사
            if (existing != null) {
                mergedConnections.addAll(existing.connections());
            }

            // 이번 비교에서 새로 찾은 연결정보를 하나씩 확인
            for (TrackCredit connection : connections) {

                // 현재 connection이 이미 mergedConnections에 있는지 여부
                boolean alreadyIncluded = false;

                // 기존에 저장된 연결정보들과 비교
                for (TrackCredit saved : mergedConnections) {

                    // 같은 사람 + 같은 역할이면 중복된 연결정보
                    if (saved.hasSamePersonAndRole(connection)) {
                        alreadyIncluded = true;

                        // 이미 같은 연결정보를 찾았으므로
                        // 더 비교할 필요 없이 안쪽 반복문 종료
                        break;
                    }
                }

                // 기존에 없는 연결정보라면 새로 추가
                if (!alreadyIncluded) {
                    mergedConnections.add(connection);
                }
            }

            // 현재 후보곡의 최신 정보를 이용해 DigCandidate 생성
            DigCandidate candidate = new DigCandidate(

                    // 후보곡의 고유 recordingId
                    track.recordingId(),

                    // 처음 등장한 후보면 현재 track의 제목 사용
                    // 이미 등록된 후보면 기존 제목 유지
                    existing == null ? track.title() : existing.title(),

                    // 기존 + 새 연결정보를 중복 없이 합친 리스트
                    mergedConnections
            );

            // recordingId를 Key로 candidate를 Map에 저장
            //
            // 같은 recordingId가 이미 존재하면
            // 기존 DigCandidate를 새 candidate로 갱신
            candidatesById.put(track.recordingId(), candidate);
        }

        // Map의 Value인 DigCandidate들만 꺼내서
        // List<DigCandidate> 형태로 변환하여 반환
        return new ArrayList<>(candidatesById.values());
    }
}

