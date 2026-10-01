package com.example.musicdigging.service;

import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Chooses a small, diverse set of recordings without treating search order as popularity. */
final class MusicRecordingSelector {
    private static final Pattern VERSION = Pattern.compile(
            "(?iu)\\b(remix|mix|live|instrumental|acoustic|demo|edit|version|sessions?|clean|explicit|remaster|mono|stereo|interlude|visualizer)\\b");
    private static final Pattern ALTERED_VERSION = Pattern.compile(
            "(?iu)\\b(remix|mix|live|instrumental|acoustic|demo|edit|sessions?|interlude|visualizer|video)\\b");
    private static final Pattern BRACKETS = Pattern.compile("\\([^)]*\\)|\\[[^]]*]");

    private static final Pattern FEATURE_JOIN = Pattern.compile("(?iu)\\b(?:feat(?:uring)?|ft)\\b\\.?");

    enum ArtistRole { PRIMARY, CO_PRIMARY, FEATURED, ABSENT }

    private MusicRecordingSelector() {}

    static List<JsonNode> select(List<JsonNode> recordings, String query, String artistId, int limit) {
        List<JsonNode> ranked = recordings.stream()
                .filter(node -> !text(node, "id").isBlank() && !text(node, "title").isBlank())
                .filter(node -> !node.path("video").asBoolean(false))
                .filter(node -> artistId == null
                        ? MusicSearchText.rank(text(node, "title"), query) <= 2
                        : artistRole(node, artistId) != ArtistRole.ABSENT)
                .sorted(Comparator
                        .comparingInt((JsonNode node) -> artistId == null
                                ? MusicSearchText.rank(text(node, "title"), query) : artistRole(node, artistId).ordinal())
                        .thenComparingInt(node -> officialReleaseCount(node, artistId) > 0 ? 0 : 1)
                        .thenComparingInt(node -> isVersion(node) ? 1 : 0)
                        .thenComparing(Comparator.comparingInt((JsonNode node) -> officialReleaseCount(node, artistId)).reversed())
                        .thenComparing(Comparator.comparingInt((JsonNode node) -> node.path("releases").size()).reversed())
                        .thenComparing(node -> text(node, "id")))
                .toList();

        // Multiple MBIDs may describe the same performance. Keep distinct version comments,
        // artist credits and substantially different lengths; never replace the chosen MBID.
        List<JsonNode> unique = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        for (JsonNode node : ranked) {
            if (!ids.add(text(node, "id"))) continue;
            if (unique.stream().noneMatch(previous -> sameRecording(previous, node))) unique.add(node);
        }
        List<JsonNode> result = new ArrayList<>();
        Set<String> selectedIds = new HashSet<>();
        Map<String, Integer> familyCounts = new LinkedHashMap<>();
        Map<String, Integer> albumCounts = new LinkedHashMap<>();
        // Finish each artist-role tier before considering less direct participation.
        // Within a tier: one version per song, two songs per album, then spare slots.
        for (int role = 0; role < (artistId == null ? 1 : ArtistRole.ABSENT.ordinal()); role++) {
            final int tier = role;
            List<JsonNode> pool = artistId == null ? unique : unique.stream()
                    .filter(node -> artistRole(node, artistId).ordinal() == tier).toList();
            for (int pass = 0; pass < 4 && result.size() < limit; pass++) {
                int familyLimit = pass % 2 == 0 ? 1 : 2;
                boolean restrictAlbum = pass < 2 && artistId != null;
                for (JsonNode node : pool) {
                    String id = text(node, "id");
                    if (selectedIds.contains(id)) continue;
                    String song = family(node) + (artistId == null ? credits(node).toString() : "");
                    if (familyCounts.getOrDefault(song, 0) >= familyLimit) continue;
                    Set<String> albums = albumKeys(node, artistId);
                    if (restrictAlbum && albums.stream().anyMatch(key -> albumCounts.getOrDefault(key, 0) >= 2)) continue;
                    result.add(node);
                    selectedIds.add(id);
                    familyCounts.merge(song, 1, Integer::sum);
                    for (String album : albums) albumCounts.merge(album, 1, Integer::sum);
                    if (result.size() == limit) break;
                }
            }
            if (result.size() == limit) break;
        }
        return List.copyOf(result);
    }

    static ArtistRole artistRole(JsonNode node, String artistId) {
        Set<String> mainArtists = new HashSet<>();
        boolean featuredSection = false, targetMain = false, targetFeatured = false;
        int unknownMain = 0;
        for (JsonNode credit : node.path("artist-credit")) {
            String id = text(credit.path("artist"), "id");
            if (!featuredSection) {
                if (id.isBlank()) unknownMain++;
                else mainArtists.add(id);
            }
            if (artistId.equals(id)) {
                if (featuredSection) targetFeatured = true;
                else targetMain = true;
            }
            // The joinphrase belongs to the current credit and introduces the next artist.
            // Once featuring starts, later comma/& joined artists remain featured.
            if (FEATURE_JOIN.matcher(text(credit, "joinphrase")).find()) featuredSection = true;
        }
        if (targetMain) return mainArtists.size() == 1 && unknownMain == 0 ? ArtistRole.PRIMARY : ArtistRole.CO_PRIMARY;
        return targetFeatured ? ArtistRole.FEATURED : ArtistRole.ABSENT;
    }

    static Set<String> albumKeys(JsonNode node, String artistId) {
        Set<String> owned = new HashSet<>(), all = new HashSet<>();
        for (JsonNode release : node.path("releases")) {
            JsonNode group = release.path("release-group");
            if (!"Album".equalsIgnoreCase(text(group, "primary-type"))) continue;
            boolean compilation = false;
            for (JsonNode type : group.path("secondary-types")) if ("Compilation".equalsIgnoreCase(type.asText())) compilation = true;
            if (compilation) continue;
            String groupId = text(group, "id");
            if (groupId.isBlank()) continue;
            // Use release-group MBIDs, so country editions do not evade the album cap.
            all.add(groupId);
            if (artistId != null && artistRole(release, artistId) != ArtistRole.ABSENT) owned.add(groupId);
        }
        return owned.isEmpty() ? all : owned;
    }

    private static int officialReleaseCount(JsonNode node, String artistId) {
        Set<String> releases = new HashSet<>();
        for (JsonNode release : node.path("releases")) {
            if (!"Official".equalsIgnoreCase(text(release, "status"))) continue;
            if (artistId != null && release.path("artist-credit").isArray()
                    && artistRole(release, artistId).ordinal() > ArtistRole.CO_PRIMARY.ordinal()) continue;
            releases.add(text(release, "id"));
        }
        return releases.size();
    }

    static String version(JsonNode node) {
        String comment = text(node, "disambiguation");
        return VERSION.matcher(comment).find() ? comment : "";
    }

    private static boolean isVersion(JsonNode node) {
        if (ALTERED_VERSION.matcher(text(node, "disambiguation")).find()) return true;
        var brackets = BRACKETS.matcher(text(node, "title"));
        while (brackets.find()) if (ALTERED_VERSION.matcher(brackets.group()).find()) return true;
        return text(node, "title").matches("(?iu).*\\s+[-–—:]\\s+.*\\b(remix|live|instrumental|edit|mix)\\b.*");
    }

    private static String family(JsonNode node) {
        String title = text(node, "title");
        var matcher = BRACKETS.matcher(title);
        StringBuilder base = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(base, VERSION.matcher(matcher.group()).find() ? "" : java.util.regex.Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(base);
        String value = base.toString().replaceAll("(?iu)\\s+[-–—:]\\s+[^:]*\\b(remix|live|instrumental|edit|version|mix)\\b.*$", "");
        return MusicSearchText.key(value);
    }

    private static boolean sameRecording(JsonNode left, JsonNode right) {
        if (!MusicSearchText.key(text(left, "title")).equals(MusicSearchText.key(text(right, "title")))) return false;
        if (!MusicSearchText.key(text(left, "disambiguation")).equals(MusicSearchText.key(text(right, "disambiguation")))) return false;
        if (!credits(left).equals(credits(right))) return false;
        int leftLength = left.path("length").asInt(0), rightLength = right.path("length").asInt(0);
        return leftLength == 0 || rightLength == 0 || Math.abs(leftLength - rightLength) <= 2000;
    }

    private static List<String> credits(JsonNode node) {
        List<String> result = new ArrayList<>();
        for (JsonNode credit : node.path("artist-credit")) {
            String id = text(credit.path("artist"), "id");
            result.add((id.isBlank() ? MusicSearchText.key(text(credit, "name")) : id)
                    + "|" + MusicSearchText.key(text(credit, "joinphrase")));
        }
        return result;
    }

    private static String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }
}
