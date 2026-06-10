package com.example.musicdigging.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DiggingResponseDto {

    private String title;
    private String artist;

    private List<CreditDto> credits;
    private List<RecommendationDto> recommendations;
}