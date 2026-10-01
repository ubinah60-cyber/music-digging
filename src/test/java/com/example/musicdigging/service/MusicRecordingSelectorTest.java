package com.example.musicdigging.service;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MusicRecordingSelectorTest {
    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode recording(String id, String title, String artistId, String comment, int length) {
        return mapper.readTree("""
                {"id":"%s","title":"%s","length":%d,"disambiguation":"%s",
                 "artist-credit":[{"name":"Artist","artist":{"id":"%s"}}]}
                """.formatted(id, title, length, comment, artistId));
    }

    @Test
    void deduplicatesPerformancesButPreservesRemixLiveAndDifferentLengths() {
        List<JsonNode> nodes = List.of(
                recording("original", "Signal", "a", "", 200000),
                recording("duplicate", "Signal", "a", "", 201000),
                recording("live", "Signal", "a", "live, 2020: London", 200000),
                recording("remix", "Signal (remix)", "a", "", 200000),
                recording("longer", "Signal", "a", "", 240000));
        var result = MusicRecordingSelector.select(nodes, "Signal", null, 8);
        // At most two versions per family, even though four distinct performances survive dedup.
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(node -> node.path("disambiguation").asText().isBlank()));
        assertEquals(0, result.stream().filter(node -> node.path("id").asText().equals("original")).count()
                * result.stream().filter(node -> node.path("id").asText().equals("duplicate")).count());
        assertFalse(MusicRecordingSelector.version(nodes.get(2)).isBlank());
        assertEquals(2, MusicRecordingSelector.select(List.of(nodes.get(0), nodes.get(2)), "Signal", "a", 8).size());
        assertEquals(2, MusicRecordingSelector.select(List.of(nodes.get(0), nodes.get(3)), "Signal", "a", 8).size());
        assertEquals(2, MusicRecordingSelector.select(List.of(nodes.get(0), nodes.get(4)), "Signal", "a", 8).size());
    }

    @Test
    void givesDifferentTitlesRoomBeforeMoreVersions() {
        List<JsonNode> nodes = new ArrayList<>();
        nodes.add(recording("original", "Signal", "a", "", 200000));
        for (int i = 0; i < 20; i++) nodes.add(recording("mix" + i, "Signal (mix " + i + ")", "a", "", 200000));
        for (int i = 0; i < 7; i++) nodes.add(recording("different" + i, "Track " + i, "a", "", 200000));
        var result = MusicRecordingSelector.select(nodes, "Artist", "a", 8);
        assertEquals(8, result.size());
        assertEquals(1, result.stream().filter(node -> node.path("title").asText().startsWith("Signal")).count());
    }

    @Test
    void prefersPrimaryArtistAndRejectsUnrelatedAndVideoRecordings() {
        JsonNode feature = mapper.readTree("""
                {"id":"feature","title":"Feature","artist-credit":[{"artist":{"id":"other"}}, {"artist":{"id":"a"}}]}
                """);
        JsonNode video = mapper.readTree("""
                {"id":"video","title":"Video","video":true,"artist-credit":[{"artist":{"id":"a"}}]}
                """);
        var result = MusicRecordingSelector.select(List.of(feature, video,
                recording("primary", "Primary", "a", "", 200000),
                recording("unrelated", "Artist Tribute", "other", "", 200000)), "Artist", "a", 8);
        assertEquals(2, result.size());
        assertEquals("primary", result.get(0).path("id").asText());
        assertEquals("feature", result.get(1).path("id").asText());
    }

    @Test
    void titleSearchPrioritizesExactTitlesAndRetainsDifferentPerformers() {
        var result = MusicRecordingSelector.select(List.of(
                recording("prefix", "Signal Fire", "a", "", 200000),
                recording("unrelated", "Something Else", "a", "", 200000),
                recording("exact", "Signal", "a", "", 200000),
                recording("other-artist", "Signal", "b", "", 200000)), "Signal", null, 8);
        assertEquals(3, result.size());
        assertEquals("Signal", result.get(0).path("title").asText());
        assertEquals("Signal", result.get(1).path("title").asText());
    }

    private JsonNode credited(String id, String creditJson) {
        return mapper.readTree("{\"id\":\"" + id + "\",\"title\":\"" + id
                + "\",\"length\":200000,\"artist-credit\":" + creditJson + "}");
    }

    private JsonNode albumTrack(String id, String album) {
        return mapper.readTree("""
                {"id":"%s","title":"%s","length":200000,"artist-credit":[{"artist":{"id":"a"}}],
                 "releases":[{"id":"edition1-%s","status":"Official","artist-credit":[{"artist":{"id":"a"}}],
                   "release-group":{"id":"%s","primary-type":"Album"}},
                 {"id":"edition2-%s","status":"Official","artist-credit":[{"artist":{"id":"a"}}],
                   "release-group":{"id":"%s","primary-type":"Album"}}]}
                """.formatted(id, id, id, album, id, album));
    }

    @Test
    void classifiesMbidAndJoinPhrasesRatherThanCreditNameOrPosition() {
        var solo = credited("solo", """
                [{"name":"Renamed Artist","artist":{"id":"a"}}]
                """);
        var withFeatures = credited("main-features", """
                [{"artist":{"id":"a"},"joinphrase":" feat. "},
                 {"artist":{"id":"b"},"joinphrase":" & "},{"artist":{"id":"c"}}]
                """);
        var joint = credited("joint", """
                [{"artist":{"id":"a"},"joinphrase":" & "},
                 {"artist":{"id":"b"},"joinphrase":" feat. "},{"artist":{"id":"c"}}]
                """);
        var jointSecond = credited("joint-second", """
                [{"artist":{"id":"b"},"joinphrase":" and "},{"artist":{"id":"a"}}]
                """);
        var featured = credited("featured", """
                [{"artist":{"id":"b"},"joinphrase":" ft. "},{"artist":{"id":"a"}}]
                """);
        assertEquals(MusicRecordingSelector.ArtistRole.PRIMARY, MusicRecordingSelector.artistRole(solo, "a"));
        assertEquals(MusicRecordingSelector.ArtistRole.PRIMARY, MusicRecordingSelector.artistRole(withFeatures, "a"));
        assertEquals(MusicRecordingSelector.ArtistRole.FEATURED, MusicRecordingSelector.artistRole(withFeatures, "c"));
        assertEquals(MusicRecordingSelector.ArtistRole.CO_PRIMARY, MusicRecordingSelector.artistRole(joint, "a"));
        assertEquals(MusicRecordingSelector.ArtistRole.CO_PRIMARY, MusicRecordingSelector.artistRole(jointSecond, "a"));
        assertEquals(MusicRecordingSelector.ArtistRole.FEATURED, MusicRecordingSelector.artistRole(featured, "a"));
        assertEquals(MusicRecordingSelector.ArtistRole.ABSENT, MusicRecordingSelector.artistRole(solo, "not-a"));
        var ranked = MusicRecordingSelector.select(List.of(featured, jointSecond, joint, withFeatures, solo), "Artist", "a", 8);
        assertEquals(List.of("main-features", "solo", "joint", "joint-second", "featured"),
                ranked.stream().map(n -> n.path("id").asText()).toList());
    }

    @Test
    void excludesComposerAndProducerRelationsWithoutRecordingArtistCredit() {
        var composer = mapper.readTree("""
                {"id":"composer-only","title":"Song","artist-credit":[{"name":"Artist A","artist":{"id":"other"}}],
                 "relations":[{"type":"composer","artist":{"id":"a"}}]}
                """);
        var producer = mapper.readTree("""
                {"id":"producer-only","title":"Song","artist-credit":[{"artist":{"id":"other"}}],
                 "relations":[{"type":"producer","artist":{"id":"a"}}]}
                """);
        assertTrue(MusicRecordingSelector.select(List.of(composer, producer), "Artist A", "a", 8).isEmpty());
    }

    @Test
    void limitsEachAlbumToTwoWhenEnoughDistinctAlbumsAreAvailable() {
        List<JsonNode> nodes = new ArrayList<>();
        for (int album = 0; album < 4; album++) {
            for (int song = 0; song < 8; song++) nodes.add(albumTrack(album + "-song-" + song, "album-" + album));
        }
        var selected = MusicRecordingSelector.select(nodes, "Artist", "a", 8);
        assertEquals(8, selected.size());
        for (int album = 0; album < 4; album++) {
            final String key = "album-" + album;
            assertEquals(2, selected.stream().filter(node -> MusicRecordingSelector.albumKeys(node, "a").contains(key)).count());
        }
    }

    @Test
    void considersCandidatesAfterTheFirstEightAndOnlyThenFillsAlbumOverflow() {
        List<JsonNode> nodes = new ArrayList<>();
        for (int i = 0; i < 8; i++) nodes.add(albumTrack("a-" + i, "album-a"));
        nodes.add(albumTrack("z-other", "album-b"));
        var selected = MusicRecordingSelector.select(nodes, "Artist", "a", 8);
        assertEquals(8, selected.size());
        assertEquals("z-other", selected.get(2).path("id").asText());
        assertEquals(7, selected.stream().filter(node -> MusicRecordingSelector.albumKeys(node, "a").contains("album-a")).count());
    }

    @Test
    void cleanAndExplicitAreStudioVersionsAndSearchScoreIsNotPopularity() {
        var studio = mapper.readTree("""
                {"id":"studio","title":"Song","score":1,"disambiguation":"explicit",
                 "artist-credit":[{"artist":{"id":"a"}}]}
                """);
        var joint = mapper.readTree("""
                {"id":"joint","title":"Other Song","score":100,
                 "artist-credit":[{"artist":{"id":"a"},"joinphrase":" & "},{"artist":{"id":"b"}}]}
                """);
        var live = recording("live", "Concert", "a", "live, 2020: London", 200000);
        var selected = MusicRecordingSelector.select(List.of(joint, live, studio), "Artist", "a", 8);
        assertEquals("studio", selected.get(0).path("id").asText());
        assertEquals("live", selected.get(1).path("id").asText());
        assertEquals("joint", selected.get(2).path("id").asText());
    }
}
