package com.example.musicdigging.digging.api;

import com.example.musicdigging.controller.DiggingController;
import com.example.musicdigging.digging.provider.SampleMusicDataProvider;
import com.example.musicdigging.digging.service.DiggingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MultiRouteDiggingApiTest {
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
            new DiggingController(new DiggingService(new SampleMusicDataProvider()))).build();

    @Test
    void existingEndpointReturnsMergedReasons() throws Exception {
        mvc.perform(get("/api/digging").param("recordingId", "track-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startRecordingId").value("track-001"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.candidates.length()").value(4))
                .andExpect(jsonPath("$.candidates[0].recordingId").value("track-002"))
                .andExpect(jsonPath("$.candidates[0].connections[0].role").value("COMPOSER"))
                .andExpect(jsonPath("$.candidates[0].reasons.length()").value(3))
                .andExpect(jsonPath("$.candidates[0].reasons[2].type").value("GENRE_AND_YEAR"))
                .andExpect(jsonPath("$.candidates[0].reasons[2].fromYear").value(2017))
                .andExpect(jsonPath("$.candidates[0].reasons[2].toYear").value(2023));
    }

    @Test
    void creditlessStartReturnsArtistCandidates() throws Exception {
        mvc.perform(get("/api/digging").param("recordingId", "track-004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.candidates.length()").value(2))
                .andExpect(jsonPath("$.candidates[0].connections.length()").value(0))
                .andExpect(jsonPath("$.candidates[0].reasons[0].type").value("SAME_ARTIST"));
    }
}
