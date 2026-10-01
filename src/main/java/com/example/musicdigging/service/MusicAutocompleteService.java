package com.example.musicdigging.service;

import com.example.musicdigging.digging.provider.MusicDataLookupException;
import com.example.musicdigging.digging.provider.musicbrainz.MusicBrainzApiClient;
import com.example.musicdigging.dto.MusicAutocompleteItem;
import com.example.musicdigging.dto.MusicAutocompleteResponse;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class MusicAutocompleteService {
    private static final int SONG_LIMIT = 8;
    private final MusicBrainzApiClient musicBrainz;
    private final ObjectMapper mapper;

    public MusicAutocompleteService(MusicBrainzApiClient musicBrainz, ObjectMapper mapper) {
        this.musicBrainz = musicBrainz;
        this.mapper = mapper;
    }

    public MusicAutocompleteResponse search(String query) {
        String normalized = query == null ? "" : query.trim();
        boolean singleCjk = normalized.matches("[가-힣一-龥ぁ-んァ-ン]");
        if ((normalized.codePointCount(0, normalized.length()) < 2 && !singleCjk)
                || normalized.length() > 80) {
            throw new IllegalArgumentException("검색어는 두 글자 이상 입력해 주세요. 한글·한자·일본어는 한 글자부터 검색할 수 있습니다.");
        }
        if (MusicSearchText.key(normalized).isBlank()) {
            throw new IllegalArgumentException("글자나 숫자가 포함된 검색어를 입력해 주세요.");
        }
        return searchMusicBrainz(normalized);
    }

    private MusicAutocompleteResponse searchMusicBrainz(String query) {
        String effective = query;
        JsonNode artists = parse(musicBrainz.searchArtistsJson(effective, 8), "artists");
        String romanized = MusicSearchText.romanizeHangul(query);
        // Check names and aliases, not just whether the native search returned something.
        if (!hasRelevantArtist(artists, query) && !romanized.equals(query)) {
            JsonNode fallback = parse(musicBrainz.searchArtistsJson(romanized, 8), "artists");
            if (hasRelevantArtist(fallback, romanized)) {
                effective = romanized;
                artists = fallback;
            }
        }
        final String searchTerm = effective;
        List<JsonNode> artistCandidates = new ArrayList<>();
        int bestArtistScore = 0;
        for (JsonNode artist : artists) bestArtistScore = Math.max(bestArtistScore, artist.path("score").asInt(0));
        for (JsonNode artist : artists) {
            int score = artist.path("score").asInt(0);
            if (bestArtistScore >= 95 && score < 95) continue;
            if (artistRank(artist, effective) > 2) continue;
            artistCandidates.add(artist);
        }
        artistCandidates.sort(Comparator.comparingInt((JsonNode artist) -> artistRank(artist, searchTerm))
                .thenComparing(Comparator.comparingInt((JsonNode artist) -> artist.path("score").asInt(0)).reversed()));
        List<MusicAutocompleteItem> artistItems = new ArrayList<>();
        for (JsonNode artist : artistCandidates.stream().limit(2).toList()) {
            String type = text(artist, "type"), country = text(artist, "country");
            add(artistItems, "artist", artist, "name", (type.isBlank() ? "아티스트" : type)
                    + (country.isBlank() ? "" : " · " + country));
        }
        JsonNode recordings = parse(musicBrainz.searchRecordingsJson(titleQuery("recording", effective), 100), "recordings");
        List<JsonNode> titleRecordings = list(recordings);
        List<MusicAutocompleteItem> titleSongs = songItems(MusicRecordingSelector.select(titleRecordings, effective, null, SONG_LIMIT));
        JsonNode releaseGroups = parse(musicBrainz.searchAlbumsJson(
                titleQuery("releasegroup", effective) + " AND primarytype:album", 100), "release-groups");
        List<JsonNode> albumCandidates = new ArrayList<>();
        for (JsonNode album : releaseGroups) {
            if (!"Album".equalsIgnoreCase(text(album, "primary-type"))) continue;
            if (MusicSearchText.rank(text(album, "title"), effective) > 2) continue;
            albumCandidates.add(album);
        }
        albumCandidates.sort(Comparator.comparingInt((JsonNode album) -> MusicSearchText.rank(text(album, "title"), searchTerm))
                .thenComparing(Comparator.comparingInt((JsonNode album) -> album.path("count").asInt(0)).reversed()));
        List<MusicAutocompleteItem> albumItems = new ArrayList<>();
        for (JsonNode album : albumCandidates.stream().limit(3).toList()) {
            String artist = artistCredit(album), date = text(album, "first-release-date");
            String description = artist.isBlank() ? "앨범" : artist;
            if (!date.isBlank()) description += " · " + date.substring(0, Math.min(4, date.length()));
            add(albumItems, "album", album, "title", description);
        }
        boolean exactAlbum = albumItems.stream().anyMatch(item -> MusicSearchText.rank(item.name(), searchTerm) == 0);
        boolean exactSong = titleSongs.stream().anyMatch(item -> MusicSearchText.rank(item.name(), searchTerm) == 0);
        int strongestExactAlbum = albumCandidates.stream()
                .filter(album -> MusicSearchText.rank(text(album, "title"), searchTerm) == 0)
                .mapToInt(album -> album.path("count").asInt(0)).max().orElse(0);
        boolean confidentArtist = !artistCandidates.isEmpty() && artistRank(artistCandidates.get(0), searchTerm) <= 1
                && artistCandidates.get(0).path("score").asInt(0) >= 95;
        boolean uniqueExactArtist = confidentArtist && artistRank(artistCandidates.get(0), searchTerm) == 0
                && artistCandidates.stream().filter(artist -> artistRank(artist, searchTerm) == 0).count() == 1;
        String focus = exactAlbum && (!exactSong || strongestExactAlbum >= 10) ? "album"
                : uniqueExactArtist ? "artist" : exactSong ? "song" : confidentArtist ? "artist" : "song";
        List<MusicAutocompleteItem> songItems = titleSongs;
        if ("artist".equals(focus)) {
            String artistId = artistItems.get(0).id();
            List<JsonNode> byArtist = list(parse(musicBrainz.searchRecordingsJson(
                    "arid:" + artistId + " AND status:official", 100), "recordings"));
            collectAlbumRecordings(byArtist, artistId);
            List<JsonNode> selected = MusicRecordingSelector.select(byArtist, searchTerm, artistId, SONG_LIMIT);
            // One bounded fallback for sparse catalogues. Never fill with unrelated title results.
            if (selected.size() < 5) {
                byArtist.addAll(list(parse(musicBrainz.searchRecordingsJson("arid:" + artistId, 100), "recordings")));
                selected = MusicRecordingSelector.select(byArtist, searchTerm, artistId, SONG_LIMIT);
            }
            songItems = songItems(selected);
            albumItems.removeIf(item -> MusicSearchText.rank(item.name(), searchTerm) != 0);
        }
        return new MusicAutocompleteResponse(List.copyOf(artistItems), songItems, List.copyOf(albumItems), "musicbrainz", focus);
    }

    private void collectAlbumRecordings(List<JsonNode> recordings, String artistId) {
        // A bounded catalogue sample, never a sweep of the artist's complete discography.
        JsonNode groups = parse(musicBrainz.searchAlbumsJson("arid:" + artistId
                + " AND primarytype:album AND status:official"
                + " NOT secondarytype:compilation NOT secondarytype:live NOT secondarytype:remix", 100), "release-groups");
        List<JsonNode> albums = list(groups).stream()
                .filter(group -> "Album".equalsIgnoreCase(text(group, "primary-type")))
                .filter(group -> MusicRecordingSelector.artistRole(group, artistId) == MusicRecordingSelector.ArtistRole.PRIMARY)
                .filter(group -> !text(group, "id").isBlank())
                .sorted(Comparator.comparingInt((JsonNode group) -> group.path("count").asInt(0)).reversed()
                        .thenComparing(group -> text(group, "first-release-date"))
                        .thenComparing(group -> text(group, "id")))
                .toList();
        java.util.Set<String> sampled = new java.util.HashSet<>();
        for (JsonNode album : albums) {
            String groupId = text(album, "id");
            if (!sampled.add(groupId)) continue;
            recordings.addAll(list(parse(musicBrainz.searchRecordingsJson("arid:" + artistId
                    + " AND rgid:" + groupId + " AND status:official", 100), "recordings")));
            if (sampled.size() == 3) break;
        }
    }

    private boolean hasRelevantArtist(JsonNode artists, String query) {
        for (JsonNode artist : artists) if (artistRank(artist, query) <= 2) return true;
        return false;
    }

    private int artistRank(JsonNode artist, String query) {
        int rank = MusicSearchText.rank(text(artist, "name"), query);
        rank = Math.min(rank, MusicSearchText.rank(text(artist, "sort-name"), query));
        for (JsonNode alias : artist.path("aliases")) rank = Math.min(rank, MusicSearchText.rank(text(alias, "name"), query));
        return rank;
    }

    private List<MusicAutocompleteItem> songItems(List<JsonNode> recordings) {
        List<MusicAutocompleteItem> result = new ArrayList<>();
        for (JsonNode recording : recordings) {
            String description = artistCredit(recording);
            String version = MusicRecordingSelector.version(recording);
            if (!version.isBlank()) description += " · " + version;
            add(result, "recording", recording, "title", description.isBlank() ? "곡" : description);
        }
        return List.copyOf(result);
    }

    private List<JsonNode> list(JsonNode nodes) {
        List<JsonNode> result = new ArrayList<>();
        for (JsonNode node : nodes) result.add(node);
        return result;
    }

    private JsonNode parse(String json, String field) {
        try {
            JsonNode items = mapper.readTree(json).path(field);
            if (!items.isArray()) throw new MusicDataLookupException("MusicBrainz 검색 응답 형식이 올바르지 않습니다.");
            return items;
        } catch (MusicDataLookupException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MusicDataLookupException("MusicBrainz 검색 응답을 읽지 못했습니다.", exception);
        }
    }

    private String titleQuery(String field, String query) {
        String cleaned = query.replaceAll("[^\\p{L}\\p{N}]+", " ").trim();
        List<String> terms = new ArrayList<>();
        for (String token : cleaned.split("\\s+")) terms.add(field + ":" + token + "*");
        return String.join(" AND ", terms);
    }

    private void add(List<MusicAutocompleteItem> items, String type, JsonNode node, String nameField, String description) {
        String id = text(node, "id"), name = text(node, nameField);
        if (!id.isBlank() && !name.isBlank()) items.add(new MusicAutocompleteItem(type, id, name, description,
                "musicbrainz", "recording".equals(type) ? id : null));
    }

    private String artistCredit(JsonNode node) {
        StringBuilder result = new StringBuilder();
        for (JsonNode credit : node.path("artist-credit")) {
            String name = text(credit, "name");
            if (name.isBlank()) name = text(credit.path("artist"), "name");
            if (!result.isEmpty() && !name.isBlank() && result.charAt(result.length() - 1) != ' ') result.append(", ");
            result.append(name).append(credit.path("joinphrase").asText(""));
        }
        return result.toString().trim();
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }
}
