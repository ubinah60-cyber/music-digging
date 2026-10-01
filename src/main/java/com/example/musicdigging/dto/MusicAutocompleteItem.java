package com.example.musicdigging.dto;

public record MusicAutocompleteItem(
        String type,
        String id,
        String name,
        String description,
        String source,
        String recordingId
) {
}
