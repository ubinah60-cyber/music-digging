package com.example.musicdigging.external.music;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class MusicBrainzService {

    private final RestClient restClient;

    public String searchArtist(String artistName) {

        String url =
                "https://musicbrainz.org/ws/2/artist/?query="
                        + artistName
                        + "&fmt=json";

        return restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);
    }
}