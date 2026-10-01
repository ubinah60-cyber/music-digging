package com.example.musicdigging.service;

import com.example.musicdigging.digging.provider.musicbrainz.MusicBrainzApiClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MusicAutocompleteServiceTest {
    private final MusicBrainzApiClient client = mock(MusicBrainzApiClient.class);
    private final MusicAutocompleteService service =
            new MusicAutocompleteService(client, new ObjectMapper());

    @org.junit.jupiter.api.BeforeEach
    void emptyAlbumSampleByDefault() {
        when(client.searchAlbumsJson(org.mockito.ArgumentMatchers.startsWith("arid:"), org.mockito.ArgumentMatchers.eq(100)))
                .thenReturn("{\"release-groups\":[]}");
    }

    @Test
    void kanyeSearchShowsYeAndItsRecordingsWithoutTributeAlbumsOrDisambiguation() {
        when(client.searchArtistsJson("Kanye", 8)).thenReturn("""
                {"artists":[{"id":"ye-id","name":"Ye","score":100,"aliases":[{"name":"Kanye"}],"type":"Person","country":"US",
                  "disambiguation":"formerly Kanye West"}]}
                """);
        when(client.searchRecordingsJson("recording:Kanye*", 100)).thenReturn("""
                {"recordings":[{"id":"other-id","title":"Kanye","artist-credit":[{"name":"Other"}]}]}
                """);
        when(client.searchAlbumsJson("releasegroup:Kanye* AND primarytype:album", 100)).thenReturn("""
                {"release-groups":[{"id":"tribute-id","title":"Kanye West Tribute","primary-type":"Album"}]}
                """);
        when(client.searchRecordingsJson("arid:ye-id AND status:official", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchRecordingsJson("arid:ye-id", 100)).thenReturn("""
                {"recordings":[{"id":"ye-song","title":"Runaway","artist-credit":[{"name":"Kanye West","artist":{"id":"ye-id"}}]}]}
                """);

        var result = service.search("Kanye");
        assertEquals("artist", result.focus());
        assertEquals("Ye", result.artists().get(0).name());
        assertEquals("Person · US", result.artists().get(0).description());
        assertFalse(result.artists().get(0).description().contains("formerly"));
        assertEquals("ye-song", result.songs().get(0).recordingId());
        assertTrue(result.albums().isEmpty());
    }

    @Test
    void koreanNameUsesRomanizedArtistLookupWhenNativeLookupIsEmpty() {
        when(client.searchArtistsJson("칸예", 8)).thenReturn("{\"artists\":[]}");
        when(client.searchArtistsJson("kanye", 8)).thenReturn("""
                {"artists":[{"id":"ye-id","name":"Ye","score":100,"aliases":[{"name":"Kanye"}]}]}
                """);
        when(client.searchRecordingsJson("recording:kanye*", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchAlbumsJson("releasegroup:kanye* AND primarytype:album", 100)).thenReturn("{\"release-groups\":[]}");
        when(client.searchRecordingsJson("arid:ye-id AND status:official", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchRecordingsJson("arid:ye-id", 100)).thenReturn("""
                {"recordings":[{"id":"runaway-id","title":"Runaway","artist-credit":[{"name":"Kanye West","artist":{"id":"ye-id"}}]}]}
                """);

        var result = service.search("칸예");
        assertEquals("Ye", result.artists().get(0).name());
        assertEquals("Runaway", result.songs().get(0).name());
        assertEquals("runaway-id", result.songs().get(0).recordingId());
    }

    @Test
    void titleSearchFiltersUnrelatedAlbumsAndRanksExactTitles() {
        when(client.searchArtistsJson("Runaway", 8)).thenReturn("{\"artists\":[{\"id\":\"band-1\",\"name\":\"Runaway\",\"score\":100},{\"id\":\"band-2\",\"name\":\"Runaway\",\"score\":100}]}");
        when(client.searchRecordingsJson("recording:Runaway*", 100)).thenReturn("""
                {"recordings":[{"id":"other","title":"The Runaway Cart"},
                  {"id":"exact","title":"Runaway","artist-credit":[{"name":"Kanye West"}]}]}
                """);
        when(client.searchAlbumsJson("releasegroup:Runaway* AND primarytype:album", 100)).thenReturn("""
                {"release-groups":[{"id":"bad","title":"A Different Album","primary-type":"Album"},
                  {"id":"related","title":"Runaway","primary-type":"Album"}]}
                """);

        var result = service.search("Runaway");
        assertEquals("song", result.focus());
        assertEquals("exact", result.songs().get(0).recordingId());
        assertEquals(1, result.albums().size());
    }

    @Test
    void graduationRanksWellRepresentedExactAlbumFirst() {
        when(client.searchArtistsJson("Graduation", 8)).thenReturn("{\"artists\":[{\"id\":\"artist\",\"name\":\"Graduation\",\"score\":100}]}");
        when(client.searchRecordingsJson("recording:Graduation*", 100)).thenReturn("""
                {"recordings":[{"id":"song","title":"Graduation"}]}
                """);
        when(client.searchAlbumsJson("releasegroup:Graduation* AND primarytype:album", 100)).thenReturn("""
                {"release-groups":[
                  {"id":"small","title":"Graduation","primary-type":"Album","count":1,"artist-credit":[{"name":"Other"}]},
                  {"id":"kanye","title":"Graduation","primary-type":"Album","count":25,"artist-credit":[{"name":"Kanye West"}]}]}
                """);
        var result = service.search("Graduation");
        assertEquals("album", result.focus());
        assertEquals("kanye", result.albums().get(0).id());
    }

    @Test
    void rejectsSingleLatinCharacter() {
        assertThrows(IllegalArgumentException.class, () -> service.search("a"));
    }

    @Test
    void nativeKoreanAliasUsesSameIdAndReturnsEightDiverseSongs() {
        when(client.searchArtistsJson("별명", 8)).thenReturn("""
                {"artists":[{"id":"artist-id","name":"Artist","score":100,"aliases":[{"name":"별명"}]}]}
                """);
        when(client.searchRecordingsJson("recording:별명*", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchAlbumsJson("releasegroup:별명* AND primarytype:album", 100)).thenReturn("{\"release-groups\":[]}");
        String songs = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> "{\"id\":\"song-" + i + "\",\"title\":\"Track " + i
                        + "\",\"artist-credit\":[{\"artist\":{\"id\":\"artist-id\"}}]}")
                .collect(java.util.stream.Collectors.joining(","));
        when(client.searchRecordingsJson("arid:artist-id AND status:official", 100))
                .thenReturn("{\"recordings\":[" + songs + "]}");
        var result = service.search("별명");
        assertEquals("artist", result.focus());
        assertEquals(8, result.songs().size());
        assertTrue(result.songs().stream().allMatch(item -> item.id().equals(item.recordingId())));
        org.mockito.Mockito.verify(client, org.mockito.Mockito.never()).searchArtistsJson("byeolmyeong", 8);
        org.mockito.Mockito.verify(client, org.mockito.Mockito.never()).searchRecordingsJson("arid:artist-id", 100);
    }

    @Test
    void highSearchScoreWithoutMatchingNameOrAliasDoesNotExpandArtist() {
        when(client.searchArtistsJson("Signal", 8)).thenReturn("""
                {"artists":[{"id":"other","name":"Unrelated","score":100}]}
                """);
        when(client.searchRecordingsJson("recording:Signal*", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchAlbumsJson("releasegroup:Signal* AND primarytype:album", 100)).thenReturn("{\"release-groups\":[]}");
        var result = service.search("Signal");
        assertTrue(result.artists().isEmpty());
        assertTrue(result.songs().isEmpty());
        org.mockito.Mockito.verify(client, org.mockito.Mockito.never()).searchRecordingsJson("arid:other AND status:official", 100);
    }

    @Test
    void sparseArtistNeverFillsWithUnrelatedTitleMatches() {
        when(client.searchArtistsJson("Artist", 8)).thenReturn("""
                {"artists":[{"id":"artist-id","name":"Artist","score":100}]}
                """);
        when(client.searchRecordingsJson("recording:Artist*", 100)).thenReturn("""
                {"recordings":[{"id":"tribute","title":"Artist Tribute","artist-credit":[{"artist":{"id":"other"}}]}]}
                """);
        when(client.searchAlbumsJson("releasegroup:Artist* AND primarytype:album", 100)).thenReturn("{\"release-groups\":[]}");
        when(client.searchRecordingsJson("arid:artist-id AND status:official", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchRecordingsJson("arid:artist-id", 100)).thenReturn("""
                {"recordings":[{"id":"related","title":"Only Track","artist-credit":[{"artist":{"id":"artist-id"}}]},
                               {"id":"bad","title":"Unrelated","artist-credit":[{"artist":{"id":"other"}}]}]}
                """);
        var result = service.search("Artist");
        assertEquals(1, result.songs().size());
        assertEquals("related", result.songs().get(0).recordingId());
    }

    @Test
    void supplementsAtMostThreeSoloArtistAlbumsAndSortsAllCollectedRecordings() {
        when(client.searchArtistsJson("Artist", 8)).thenReturn("{\"artists\":[{\"id\":\"artist-id\",\"name\":\"Artist\",\"score\":100}]}");
        when(client.searchRecordingsJson("recording:Artist*", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchAlbumsJson("releasegroup:Artist* AND primarytype:album", 100)).thenReturn("{\"release-groups\":[]}");
        when(client.searchRecordingsJson("arid:artist-id AND status:official", 100)).thenReturn("{\"recordings\":[]}");
        when(client.searchAlbumsJson("arid:artist-id AND primarytype:album AND status:official NOT secondarytype:compilation NOT secondarytype:live NOT secondarytype:remix", 100)).thenReturn("""
                {"release-groups":[
                  {"id":"joint","title":"Joint","primary-type":"Album","count":100,
                   "artist-credit":[{"artist":{"id":"artist-id"},"joinphrase":" & "},{"artist":{"id":"other"}}]},
                  {"id":"a","title":"A","primary-type":"Album","count":40,"artist-credit":[{"artist":{"id":"artist-id"}}]},
                  {"id":"b","title":"B","primary-type":"Album","count":30,"artist-credit":[{"artist":{"id":"artist-id"}}]},
                  {"id":"c","title":"C","primary-type":"Album","count":20,"artist-credit":[{"artist":{"id":"artist-id"}}]},
                  {"id":"d","title":"D","primary-type":"Album","count":10,"artist-credit":[{"artist":{"id":"artist-id"}}]}]}
                """);
        String albumSongs = "{\"recordings\":[" + java.util.stream.IntStream.range(0, 8)
                .mapToObj(i -> "{\"id\":\"song-" + i + "\",\"title\":\"Track " + i
                        + "\",\"artist-credit\":[{\"artist\":{\"id\":\"artist-id\"}}]}")
                .collect(java.util.stream.Collectors.joining(",")) + "]}";
        for (String album : java.util.List.of("a", "b", "c")) {
            when(client.searchRecordingsJson("arid:artist-id AND rgid:" + album + " AND status:official", 100)).thenReturn(albumSongs);
        }
        var result = service.search("Artist");
        assertEquals(8, result.songs().size());
        org.mockito.Mockito.verify(client, org.mockito.Mockito.never()).searchRecordingsJson("arid:artist-id AND rgid:joint AND status:official", 100);
        org.mockito.Mockito.verify(client, org.mockito.Mockito.never()).searchRecordingsJson("arid:artist-id AND rgid:d AND status:official", 100);
        org.mockito.Mockito.verify(client, org.mockito.Mockito.never()).searchRecordingsJson("arid:artist-id", 100);
    }
}
