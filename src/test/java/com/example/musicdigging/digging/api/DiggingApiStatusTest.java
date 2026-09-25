// 디깅 API가 후보 없음과 외부 음악 데이터 조회 실패를 구분해 응답하는지 검증한다.
package com.example.musicdigging.digging.api;

import com.example.musicdigging.controller.DiggingController;
import com.example.musicdigging.controller.DiggingExceptionHandler;
import com.example.musicdigging.digging.provider.MusicDataLookupException;
import com.example.musicdigging.digging.provider.SampleMusicDataProvider;
import com.example.musicdigging.digging.service.DiggingService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiggingApiStatusTest {

    // 시작곡은 있지만 추천 후보가 없으면 NO_CANDIDATES를 반환한다.
    @Test
    void returnsNoCandidatesWhenNothingMatches() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new DiggingController(
                        new DiggingService(new SampleMusicDataProvider())
                )
        ).setControllerAdvice(new DiggingExceptionHandler()).build();

        mvc.perform(get("/api/digging").param("recordingId", "track-007"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NO_CANDIDATES"))
                .andExpect(jsonPath("$.candidates.length()").value(0));
    }

    // 외부 음악 데이터 조회 실패는 후보 없음과 구분해 502로 반환한다.
    @Test
    void returnsBadGatewayWhenMusicDataLookupFails() throws Exception {
        DiggingService service = mock(DiggingService.class);
        when(service.dig("server-error"))
                .thenThrow(new MusicDataLookupException(
                        "MusicBrainz 요청에 실패했습니다."
                ));

        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                new DiggingController(service)
        ).setControllerAdvice(new DiggingExceptionHandler()).build();

        mvc.perform(get("/api/digging").param("recordingId", "server-error"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("MUSIC_DATA_LOOKUP_FAILED"));
    }
}