package com.example.musicdigging.digging.model;

import java.util.List;

public record CreditEvidence(
        TrackCredit credit,
        String provider,
        String sourceEntityType,
        String sourceEntityId,
        String relationshipTypeId,
        List<String> attributes
) {
    public CreditEvidence {
        attributes = List.copyOf(attributes);
    }
}