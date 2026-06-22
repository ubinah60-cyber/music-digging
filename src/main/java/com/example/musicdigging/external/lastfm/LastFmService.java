package com.example.musicdigging.external.lastfm;

import com.example.musicdigging.dto.SimilarArtistDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LastFmService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${lastfm.api-key}")
    private String apiKey;

    @Value("${lastfm.api-url}")
    private String apiUrl;

    public List<SimilarArtistDto> getSimilarArtists(String artistName) {

        String url =
                apiUrl
                        + "?method=artist.getsimilar"
                        + "&artist=" + artistName
                        + "&api_key=" + apiKey
                        + "&format=json";

        String json =
                restClient.get()
                        .uri(url)
                        .retrieve()
                        .body(String.class);

        try {
            JsonNode root =
                    objectMapper.readTree(json);

            JsonNode artists =
                    root.path("similarartists").path("artist");

            List<SimilarArtistDto> result =
                    new ArrayList<>();

            for (JsonNode artist : artists) {

                SimilarArtistDto dto =
                        new SimilarArtistDto();

                dto.setName(artist.path("name").asText(null));
                dto.setMatch(artist.path("match").asText(null));
                dto.setUrl(artist.path("url").asText(null));

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}