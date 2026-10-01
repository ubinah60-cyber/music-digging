package com.example.musicdigging.dto;

import java.util.List;

public record MusicAutocompleteResponse(
        List<MusicAutocompleteItem> artists,
        List<MusicAutocompleteItem> songs,
        List<MusicAutocompleteItem> albums,
        String source,
        String focus
) {
}
