package com.example.musicdigging.external.music;

import com.example.musicdigging.dto.AlbumDto;
import com.example.musicdigging.dto.ArtistDto;
import com.example.musicdigging.dto.TrackDto;
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

    public List<AlbumDto> searchAlbums(String artistName) {

        String url =
                "https://musicbrainz.org/ws/2/release-group/?query=artist:"
                        + artistName
                        + "&fmt=json";

        String json = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode releaseGroups = root.get("release-groups");

            List<AlbumDto> result = new ArrayList<>();

            for (JsonNode releaseGroup : releaseGroups) {
                AlbumDto dto = new AlbumDto();

                dto.setTitle(releaseGroup.path("title").asText());
                dto.setType(releaseGroup.path("primary-type").asText(null));
                dto.setFirstReleaseDate(releaseGroup.path("first-release-date").asText(null));
                dto.setId(releaseGroup.path("id").asText());

                result.add(dto);
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("MusicBrainz 앨범 응답 파싱 실패", e);
        }
    }

    public AlbumDto getAlbumDetail(String albumId) {

        String url =
                "https://musicbrainz.org/ws/2/release-group/"
                        + albumId
                        + "?inc=releases&fmt=json";

        String json = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(json);

            AlbumDto dto = new AlbumDto();

            dto.setId(root.path("id").asText());
            dto.setTitle(root.path("title").asText());
            dto.setType(root.path("primary-type").asText(null));
            dto.setFirstReleaseDate(root.path("first-release-date").asText(null));

            JsonNode releases = root.path("releases");

            if (releases.isArray() && releases.size() > 0) {
                dto.setReleaseId(
                        releases.get(0)
                                .path("id")
                                .asText(null)
                );
            }

            return dto;

        } catch (Exception e) {
            throw new RuntimeException("MusicBrainz 앨범 상세 응답 파싱 실패", e);
        }
    }

    public List<TrackDto> getTracks(String releaseId) {

        String url =
                "https://musicbrainz.org/ws/2/release/"
                        + releaseId
                        + "?inc=recordings&fmt=json";

        String json = restClient.get()
                .uri(url)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode media = root.path("media");

            List<TrackDto> result = new ArrayList<>();

            for (JsonNode medium : media) {
                JsonNode tracks = medium.path("tracks");

                for (JsonNode track : tracks) {
                    TrackDto dto = new TrackDto();

                    dto.setTrackNumber(track.path("number").asInt());
                    dto.setTitle(track.path("title").asText(null));

                    long length = track.path("length").asLong();

                    long seconds = length / 1000;
                    long minutes = seconds / 60;
                    long remainSeconds = seconds % 60;

                    dto.setLength(
                            String.format("%d:%02d", minutes, remainSeconds)
                    );

                    result.add(dto);
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("MusicBrainz 트랙 응답 파싱 실패", e);
        }
    }
}