// 역할: musicbrainz 프로필에서 실제 MusicBrainz 데이터 제공자를 Spring 빈으로 등록한다.
package com.example.musicdigging.config;

import com.example.musicdigging.digging.provider.MusicDataProvider;
import com.example.musicdigging.digging.provider.musicbrainz.MusicBrainzApiClient;
import com.example.musicdigging.digging.provider.musicbrainz.MusicBrainzDataProvider;
import com.example.musicdigging.digging.provider.musicbrainz.MusicBrainzRecordingParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import tools.jackson.databind.ObjectMapper;

@Configuration
@Profile("musicbrainz")
public class MusicBrainzProviderConfig {

    // 샘플 Provider와 함께 등록돼도 실제 MusicBrainz Provider를 선택한다.
    @Bean
    @Primary
    public MusicDataProvider musicBrainzDataProvider(
            MusicBrainzApiClient client
    ) {
        return new MusicBrainzDataProvider(
                client,
                new MusicBrainzRecordingParser(),
                new ObjectMapper()
        );
    }
}