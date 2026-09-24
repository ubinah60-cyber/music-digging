package com.example.musicdigging.digging.model;

public enum DiggingStatus {
    SUCCESS,
    TRACK_NOT_FOUND,
    // 이전 응답과의 호환을 위해 유지. 다중 경로 수집에서는 반환하지 않는다.
    NO_CREDITS,
    NO_CANDIDATES
}
