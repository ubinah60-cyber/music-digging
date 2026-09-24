package com.example.musicdigging.config;

import com.example.musicdigging.digging.provider.MusicDataProvider;
import com.example.musicdigging.digging.service.DiggingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("local")
public class DiggingConfig {

    @Bean
    public DiggingService diggingService(MusicDataProvider provider) {
        return new DiggingService(provider);
    }
}