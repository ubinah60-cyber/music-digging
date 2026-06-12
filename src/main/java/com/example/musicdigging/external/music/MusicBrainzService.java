package com.example.musicdigging.external.music;

import com.example.musicdigging.dto.ArtistDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MusicBrainzService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public List<ArtistDto> searchArtist(String artistName) {

        String url =
                "https://musicbrainz.org/ws/2/artist/?query="
                        + artistName
                        + "&fmt=json";

        String json = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode artists = root.get("artists");

            List<ArtistDto> result = new ArrayList<>();

            for (JsonNode artist : artists) {
                ArtistDto dto = new ArtistDto();

                dto.setName(artist.path("name").asText());
                dto.setCountry(artist.path("country").asText(null));
                dto.setType(artist.path("type").asText(null));

                result.add(dto);

                break;
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("MusicBrainz 응답 파싱 실패", e);
        }
    }
}