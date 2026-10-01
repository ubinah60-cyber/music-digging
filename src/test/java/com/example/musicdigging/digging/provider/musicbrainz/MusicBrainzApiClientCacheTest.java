package com.example.musicdigging.digging.provider.musicbrainz;

import com.example.musicdigging.digging.provider.MusicDataLookupException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class MusicBrainzApiClientCacheTest {
    @Test
    void cachesEquivalentArtistQueriesAndExpiresSuccesses() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        AtomicLong clock = new AtomicLong(0);
        var client = new MusicBrainzApiClient(builder.build(), clock::get);
        server.expect(header("User-Agent", "Digger/0.1 (https://github.com/ubinah60-cyber/music-digging)"))
                .andRespond(withSuccess("{\"artists\":[]}", MediaType.APPLICATION_JSON));
        server.expect(header("User-Agent", "Digger/0.1 (https://github.com/ubinah60-cyber/music-digging)"))
                .andRespond(withSuccess("{\"artists\":[1]}", MediaType.APPLICATION_JSON));
        assertEquals("{\"artists\":[]}", client.searchArtistsJson("Example", 8));
        assertEquals("{\"artists\":[]}", client.searchArtistsJson(" example ", 8));
        clock.set(TimeUnit.MINUTES.toNanos(10));
        assertEquals("{\"artists\":[1]}", client.searchArtistsJson("EXAMPLE", 8));
        server.verify();
    }

    @Test
    void concurrentIdenticalRequestsOnlyCallUpstreamOnce() throws Exception {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new MusicBrainzApiClient(builder.build());
        server.expect(header("User-Agent", "Digger/0.1 (https://github.com/ubinah60-cyber/music-digging)"))
                .andRespond(withSuccess("{\"recordings\":[]}", MediaType.APPLICATION_JSON));
        var pool = Executors.newFixedThreadPool(2);
        try {
            var first = pool.submit(() -> client.searchRecordingsJson("arid:artist-id", 100));
            var second = pool.submit(() -> client.searchRecordingsJson("arid:artist-id", 100));
            assertEquals(first.get(5, TimeUnit.SECONDS), second.get(5, TimeUnit.SECONDS));
            server.verify();
        } finally { pool.shutdownNow(); }
    }

    @Test
    void failuresAreRetriedAndUncachedCallsKeepTheRequestInterval() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new MusicBrainzApiClient(builder.build());
        server.expect(header("User-Agent", "Digger/0.1 (https://github.com/ubinah60-cyber/music-digging)"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(header("User-Agent", "Digger/0.1 (https://github.com/ubinah60-cyber/music-digging)"))
                .andRespond(withSuccess("{\"artists\":[]}", MediaType.APPLICATION_JSON));
        long start = System.nanoTime();
        assertThrows(MusicDataLookupException.class, () -> client.searchArtistsJson("Example", 8));
        assertEquals("{\"artists\":[]}", client.searchArtistsJson("Example", 8));
        assertTrue(System.nanoTime() - start >= TimeUnit.SECONDS.toNanos(1));
        server.verify();
    }
}
