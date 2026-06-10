package com.example.musicdigging.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecommendationDto {

    private String title;
    private String artist;
    private String reason; // "같은 프로듀서", "같은 작곡가"
}