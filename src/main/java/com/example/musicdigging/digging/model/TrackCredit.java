package com.example.musicdigging.digging.model;

import java.util.Objects;

public record TrackCredit(
        String personId,
        String personName,
        CreditRole role
) {
    public TrackCredit {
        if (personId == null || personId.isBlank()) {
            throw new IllegalArgumentException("참여자 ID는 필수입니다.");
        }

        Objects.requireNonNull(role, "참여 역할은 필수입니다.");
    }

    public boolean hasSamePersonAndRole(TrackCredit other) {
        return other != null
                && personId.equals(other.personId())
                && role == other.role();
    }
}